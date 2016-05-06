package org.infinispan.atomic.container;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.ReadOnly;
import org.infinispan.atomic.object.*;
import org.infinispan.atomic.utils.ThreadLocalUUIDGenerator;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.infinispan.atomic.object.Utils.initObject;
import static org.infinispan.atomic.object.Utils.isCompatible;

/**
 * @author Pierre Sutra
  */
public abstract class BaseContainer extends AbstractContainer {

   // object's fields
   private AtomicInteger pendingCalls;
   private boolean isOpen, isClosed;
   private Reference reference;

   public BaseContainer(Class clazz, Object key, final boolean readOptimization,
         final boolean forceNew, final Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException,
         java.util.concurrent.TimeoutException, NoSuchFieldException {

      super(clazz, readOptimization, forceNew, initArgs);
      this.pendingCalls = new AtomicInteger();
      this.isOpen = false;
      this.isClosed = false;

      // build the proxy
      MethodHandler handler = new BaseContainerMethodHandler(this);
      ProxyFactory fact = new ProxyFactory();
      fact.setSuperclass(clazz);
      fact.setFilter(methodFilter);
      fact.setInterfaces(new Class[] { WriteReplace.class });
      fact.setUseWriteReplace(false);
      this.proxy = initObject(fact.createClass(), initArgs);
      ((ProxyObject) proxy).setHandler(handler);

      // build reference and set key
      if (clazz.getAnnotation(Distributed.class)!=null) {
         String fieldName = ((Distributed) clazz.getAnnotation(Distributed.class)).key();
         Field field = clazz.getDeclaredField(fieldName);
         if (key == null) {
            key = field.get(proxy);
            assert key != null : " field " + fieldName + " is null for "+clazz;
         } else  {
            field.set(proxy,key);
         }

      }

      this.reference = new Reference(clazz,key);

   }

   @Override
   public synchronized void open()
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (!isOpen) {

         if (log.isTraceEnabled())
            log.trace(" Opening.");

         execute(new CallOpen(listenerID(), UUID.randomUUID(), forceNew, initArgs, readOptimization));
         isOpen = true;

         if (log.isTraceEnabled())
            log.trace(" Opened.");

      }

   }

   @Override
   public synchronized void close()
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (log.isTraceEnabled())
         log.trace(" Closing.");

      while (pendingCalls.get() != 0) {
         this.wait();
      }

      if (isOpen) {

         execute(new CallClose(listenerID(), UUID.randomUUID()));
         isOpen = false;
         forceNew = false;
         isClosed = true;

      }

      if (log.isTraceEnabled())
         log.trace(" Closed.");

   }

   @Override
   public synchronized boolean isClosed(){
      return !isOpen;
   }

   @Override
   public Reference getReference() {
      return  this.reference;
   };

   @Override
   public String toString(){
      return "Container["+listenerID().toString().substring(0,5)+":"+getReference()+"]";
   }

   private class BaseContainerMethodHandler implements MethodHandler, Serializable{

      BaseContainer container;

      public BaseContainerMethodHandler(BaseContainer container) {
         this.container = container;
      }

      public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {

         if(log.isTraceEnabled())
            log.trace("Calling "+reference.getClazz()+"."+m.getName()+"("+ Arrays.toString(args)+")");

         if (m.getName().equals("equals")) {
            if (args[0] == null) {
               return false;
            } else if (args[0] == proxy) {
               return true;
            } else if (args[0] instanceof Reference) {
               return reference.equals(args[0]);
            } else if (ProxyFactory.isProxyClass(args[0].getClass())) {
               return args[0].equals(reference); // FIXME might not be the most satisfying
            }
            return args[0].equals(proxy);
         }

         if (m.getName().equals("toString")) {
            return reference.toString();
         }

         if (m.getName().equals("hashCode")) {
            return reference.hashCode();
         }

         if (m.getName().equals("writeReplace")) {
            if (!isClosed) open(); // mandatory to create the object remotely
            return reference;
         }

         if (! Utils.isMethodSupported(reference.getClazz(), m)) {
            throw new IllegalArgumentException("Unsupported method "+m.getName()+" in "+reference.getClazz());
         }
         
         if (readOptimization 
               && state != null
               && (m.isAnnotationPresent(ReadOnly.class))) {
            if (log.isTraceEnabled()) log.trace("local call: "+m.getName());
            return Utils.callObject(state,m.getName(),args);
         }else{
            if (log.isTraceEnabled())
               log.trace("remote call: "+m.getName()+";reason: +"
                     + "null state="+new Boolean(state==null)+", "
                     + "isAnnotationPresent="+new Boolean(m.isAnnotationPresent(ReadOnly.class)));
         }

         pendingCalls.incrementAndGet();

         open();

         // handle UUID generator
         RandomBasedGenerator generator = ThreadLocalUUIDGenerator.getThreadLocal();
         UUID uuid = generator==null ? UUID.randomUUID() : generator.generate();
         if (log.isTraceEnabled()) {
               log.trace("generated " + uuid + " m=" + m.getName()
                     + ", reference=" + reference + "[" + ((generator == null) ? "null" : generator.toString())+ "]");
         }

         Object ret = execute(
               new CallInvoke(
                     listenerID(),
                     uuid,
                     m.getName(),
                     args)
         );

         if (pendingCalls.decrementAndGet()==0) {
            synchronized (container) {
               container.notifyAll();
            }
         }

         ret = Reference.unreference(ret, getCache());

         assert (m.getReturnType().equals(Void.TYPE) && ret==null) || isCompatible(ret,m.getReturnType())
               : m.getReturnType()+" => "+ret.getClass() + " ["+reference.getClazz()+"."+m.getName()+"()]";

         return ret;

      }

      @Override
      public String toString(){
         return "MethodHandler ["+getReference()+"]";
      }

   }
   
   public interface WriteReplace {
      Object writeReplace() throws java.io.ObjectStreamException;
   }

}
