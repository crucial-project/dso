package org.infinispan.creson.container;

import javassist.util.proxy.MethodFilter;
import org.infinispan.creson.object.Call;
import org.infinispan.creson.object.CallFuture;
import org.infinispan.creson.object.Reference;
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

import static org.infinispan.creson.object.Utils.hasReadOnlyMethods;

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
   public static final int MAX_ATTEMPTS = 3;
   protected static final Map<UUID, CallFuture> registeredCalls = new ConcurrentHashMap<>();
   protected static final Log log = LogFactory.getLog(BaseContainer.class);
   protected static final MethodFilter methodFilter = new MethodFilter() {
      @Override
      public boolean isHandled(Method m) {
         return !m.getName().equals("finalize");
      }
   };

   protected boolean readOptimization;
   protected Object proxy;
   protected Object state;
   protected boolean forceNew;
   protected Object[] initArgs;
   protected UUID listenerID;

   public AbstractContainer(
         Class clazz,
         final boolean readOptimization,
         final boolean forceNew,
         final Object... initArgs){
      this.readOptimization = readOptimization && hasReadOnlyMethods(clazz);
      this.forceNew = forceNew;
      this.initArgs = initArgs;
      this.listenerID = UUID.randomUUID();
   }

   public final Object getProxy(){
      return proxy;
   }

   public abstract Reference getReference();

   public abstract void open()
         throws InterruptedException, ExecutionException, TimeoutException, IOException;

   public abstract void close()
         throws InterruptedException, ExecutionException, TimeoutException, IOException;

   public abstract BasicCache getCache();

   public UUID listenerID() {
      return listenerID;
   }

   public abstract void execute(Reference reference, Call call);

   protected Object execute(Call call)
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (log.isTraceEnabled()) 
         log.trace(this + " Executing "+call);

      CallFuture future = new CallFuture(call.getCallID(), listenerID());

      registeredCalls.put(call.getCallID(), future);

      Object ret = null;
      int attempts = 0;
      while(!future.isDone()) {
         try {
            attempts++;
            execute(getReference(), call);
            ret = future.get(TTIMEOUT_TIME, TimeUnit.MILLISECONDS);
            if (ret instanceof Throwable)
               throw new ExecutionException((Throwable) ret);
         }catch (TimeoutException e) {
            if (!future.isDone())
               log.warn(" Failed "+ call + " ("+e.getMessage()+")");
            if (attempts==MAX_ATTEMPTS) {
               registeredCalls.remove(call.getCallID());
               throw new TimeoutException(call + " failed");
            }
            Thread.sleep(TTIMEOUT_TIME);
         }
      }

      registeredCalls.remove(call.getCallID());

      if (readOptimization && future.getState()!=null ) {
         this.state = future.getState();
      }

      if (log.isTraceEnabled())
         log.trace(this + " Returning " + ret);
      
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
