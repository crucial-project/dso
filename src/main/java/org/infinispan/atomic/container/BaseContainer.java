package org.infinispan.atomic.container;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.ReadOnly;
import org.infinispan.atomic.object.*;
import org.infinispan.atomic.utils.UUIDGenerator;
import org.infinispan.commons.api.BasicCache;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.infinispan.atomic.object.Reference.unreference;
import static org.infinispan.atomic.object.Utils.initObject;

/**
 * @author Pierre Sutra
  */
public abstract class BaseContainer extends AbstractContainer {

   // object's fields
   private AtomicInteger pendingCalls;
   private boolean isOpen;

   public BaseContainer(final BasicCache<Reference,Call> c, Reference reference, final boolean readOptimization,
         final boolean forceNew, final Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException,
         java.util.concurrent.TimeoutException {

      super(c, reference, readOptimization, forceNew, initArgs);
      this.pendingCalls = new AtomicInteger();
      this.isOpen = false;

      // build the proxy
      MethodHandler handler = new BaseContainerMethodHandler();
      ProxyFactory fact = new ProxyFactory();
      fact.setSuperclass(reference.getClazz());
      fact.setFilter(methodFilter);
      fact.setInterfaces(new Class[] { WriteReplace.class });
      fact.setUseWriteReplace(false);
      this.proxy = initObject(fact.createClass(),initArgs);
      ((ProxyObject)proxy).setHandler(handler);
   }
   
   @Override
   public synchronized void open() 
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      pendingCalls.incrementAndGet();
      
      if (!isOpen) {

         if (log.isTraceEnabled()) log.trace(this + "Opening.");
         
         execute(new CallOpen(listenerID(), UUIDGenerator.generate(), forceNew, initArgs, readOptimization));
         isOpen = true;

         if (log.isTraceEnabled()) log.trace(this+  "Opened.");
      }      
      
   }

   @Override
   public synchronized void close()
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (log.isTraceEnabled()) log.trace(this + "Closing.");

      while(pendingCalls.get()!=0);

      if (isOpen) {

         isOpen = false;
         execute(new CallClose(listenerID(), UUIDGenerator.generate()));
         forceNew = false;

      }

      if (log.isTraceEnabled()) log.trace(this + "Closed.");

   }

   @Override
   public String toString(){
      return "Container["+listenerID()+":"+getReference()+"]";
   }

   private class BaseContainerMethodHandler implements MethodHandler, Serializable{

      public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {

         if (m.getName().equals("equals") && args[0] == proxy)
            return true;

         if (m.getName().equals("writeReplace")) {
            return reference;
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
         
         open();

         Object ret = execute(
               new CallInvoke(
                     listenerID(),
                     UUIDGenerator.getThreadLocal()==null ?
                           UUIDGenerator.generate() : UUIDGenerator.getThreadLocal().generate(),
                     m.getName(),
                     args)
         );


         pendingCalls.decrementAndGet();

         return (ret instanceof Reference)
               ? unreference((Reference)ret,AtomicObjectFactory.forCache(cache)) : ret;

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
