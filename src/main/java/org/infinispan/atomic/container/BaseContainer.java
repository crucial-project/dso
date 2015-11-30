package org.infinispan.atomic.container;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.ReadOnly;
import org.infinispan.atomic.object.*;
import org.infinispan.atomic.utils.UUIDGenerator;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.infinispan.atomic.object.Utils.initObject;

/**
 * @author Pierre Sutra
  */
public abstract class BaseContainer extends AbstractContainer {

   // object's fields
   private AtomicInteger pendingCalls;
   private boolean isOpen;
   private Reference reference;

   public BaseContainer(Class clazz, Object key, final boolean readOptimization,
         final boolean forceNew, final Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException,
         java.util.concurrent.TimeoutException, NoSuchFieldException {

      super(clazz, readOptimization, forceNew, initArgs);
      this.pendingCalls = new AtomicInteger();
      this.isOpen = false;

      // build the proxy
      MethodHandler handler = new BaseContainerMethodHandler(this);
      ProxyFactory fact = new ProxyFactory();
      fact.setSuperclass(clazz);
      fact.setFilter(methodFilter);
      fact.setInterfaces(new Class[] { WriteReplace.class });
      fact.setUseWriteReplace(false);
      this.proxy = initObject(fact.createClass(), initArgs);
      ((ProxyObject) proxy).setHandler(handler);

      // build reference
      if (key==null) {
         String fieldName = ((Distributed) clazz.getAnnotation(Distributed.class)).key();
         key = clazz.getDeclaredField(fieldName).get(proxy);
         assert key!=null : " field " +fieldName+" is null";
      }
      this.reference = new Reference(clazz,key);

   }

   @Override
   public synchronized void open()
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (!isOpen) {

         if (log.isTraceEnabled())
            log.trace(this + " Opening.");

         execute(new CallOpen(listenerID(), UUIDGenerator.generate(), forceNew, initArgs, readOptimization));
         isOpen = true;

         if (log.isTraceEnabled())
            log.trace(this + " Opened.");

      }

   }

   @Override
   public synchronized void close()
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (log.isTraceEnabled())
         log.trace(this + " Closing.");

      while (pendingCalls.get() != 0) {
         this.wait();
      }

      if (isOpen) {

         execute(new CallClose(listenerID(), UUIDGenerator.generate()));
         isOpen = false;
         forceNew = false;

      }

      if (log.isTraceEnabled())
         log.trace(this + " Closed.");

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

         if (m.getName().equals("equals") && args[0] == proxy)
            return true;

         if (m.getName().equals("writeReplace")) {
            open(); // mandatory to create the object remotely
            return reference;
         }

         if (m.getName().equals("toString")) {
            return "";
         }

         if (! Utils.isMethodSupported(reference.getClazz(), m)) {
            throw new IllegalArgumentException("Unsupported methd "+m.getName()+" in "+reference.getClazz());
         }

         
         if (readOptimization 
               && state != null
               && (m.isAnnotationPresent(ReadOnly.class))) {
            if (log.isTraceEnabled()) log.trace(this + "local call: "+m.getName());
            return Utils.callObject(state,m.getName(),args);
         }else{
            if (log.isTraceEnabled())
               log.trace(this + "remote call: "+m.getName()+";reason: +"
                     + "null state="+new Boolean(state==null)+", "
                     + "isAnnotationPresent="+new Boolean(m.isAnnotationPresent(ReadOnly.class)));
         }

         pendingCalls.incrementAndGet();

         open();

         RandomBasedGenerator generator = UUIDGenerator.getThreadLocal();
         UUID uuid = generator==null ? UUIDGenerator.generate() : generator.generate();
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

         return Reference.unreference(ret, getCache());

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
