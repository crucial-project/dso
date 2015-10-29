package org.infinispan.atomic.container;

import javassist.util.proxy.MethodFilter;
import org.infinispan.atomic.object.Call;
import org.infinispan.atomic.object.CallFuture;
import org.infinispan.atomic.object.Reference;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.infinispan.atomic.object.Utils.hasReadOnlyMethods;

/**
 *
 * Uninstalling a listener is not necessary, as when all clients disconnect, 
 * it is automatically removed. 
 * 
 * @author Pierre Sutra
 */
public abstract class AbstractContainer {

   // class fields
   public static final int TTIMEOUT_TIME = 1000;
   protected static final Map<UUID, CallFuture> registeredCalls = new ConcurrentHashMap<>();
   protected static final Log log = LogFactory.getLog(BaseContainer.class);
   protected static final MethodFilter methodFilter = new MethodFilter() {
      @Override
      public boolean isHandled(Method m) {
         return !m.getName().equals("finalize");
      }
   };

   protected BasicCache<Reference,Call> cache;
   protected final Reference reference;
   protected boolean readOptimization;
   protected Object proxy;
   protected Object state;
   protected boolean forceNew;
   protected Object[] initArgs;

   public AbstractContainer(
         final BasicCache cache,
         final Reference reference,
         final boolean readOptimization,
         final boolean forceNew,
         final Object... initArgs){
      this.cache = cache;
      this.reference = reference;
      this.readOptimization = readOptimization && hasReadOnlyMethods(reference.getClazz());
      this.forceNew = forceNew;
      this.initArgs = initArgs;
   }

   public final Object getProxy(){
      return proxy;
   }

   public final Object getReference(){
      return reference.getKey();
   }

   public abstract void open()
         throws InterruptedException, ExecutionException, TimeoutException, IOException;

   public abstract void close()
         throws InterruptedException, ExecutionException, TimeoutException, IOException;

   public abstract UUID listenerID();

   protected Object execute(Call call)
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (log.isTraceEnabled()) 
         log.trace(this + "Executing " + call);

      CallFuture future = new CallFuture(call.getCallID());
      registeredCalls.put(call.getCallID(), future);

      Object ret = null;
      while(!future.isDone()) {
         try {
            cache.put(reference, call);
            ret = future.get(TTIMEOUT_TIME, TimeUnit.MILLISECONDS);
         }catch (Exception e) {
            if (!future.isDone()) {
               e.printStackTrace();
            }
            Thread.sleep(1000);
         }
      }

      registeredCalls.remove(call.getCallID());

      if (readOptimization && future.getState()!=null ) {
         this.state = future.getState();
      }

      if (log.isTraceEnabled()) 
         log.trace(this + "Returning " + ret);
      
      return ret;

   }


   protected static void handleFuture(CallFuture future){
      try {
         assert (future.isDone());
         if (!registeredCalls.containsKey(future.getCallID())) {
            log.trace("Future " + future.getCallID() + " ignored");
            return; // duplicate received
         }
         CallFuture clientFuture = registeredCalls.get(future.getCallID());
         assert (clientFuture!=null);
         registeredCalls.remove(future.getCallID());
         clientFuture.setState(future.getState());
         clientFuture.set(future.get());
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

}
