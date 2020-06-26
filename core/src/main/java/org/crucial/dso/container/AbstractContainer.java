package org.crucial.dso.container;

import javassist.util.proxy.MethodFilter;
import org.crucial.dso.object.Call;
import org.crucial.dso.object.CallResponse;
import org.crucial.dso.object.Reference;
import org.crucial.dso.utils.Reflection;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * Uninstalling a listener is not necessary, as when all clients disconnect,
 * it is automatically removed.
 *
 * @author Pierre Sutra
 */
public abstract class AbstractContainer {

   public static final int TTIMEOUT_TIME = 1000;
   public static final int MAX_ATTEMPTS = 3;
   protected static final Map<UUID, CompletableFuture<CallResponse>> registeredCalls = new ConcurrentHashMap<>();
   protected static final Log log = LogFactory.getLog(AbstractContainer.class);
   protected static final MethodFilter methodFilter = m -> !m.getName().equals("finalize");

   protected boolean readOptimization;
   protected boolean isIdempotent;
   protected Object proxy;
   protected Object state;
   protected boolean forceNew;
   protected Object[] initArgs;

   public AbstractContainer(
         Class clazz,
         final boolean readOptimization,
         final boolean isIdempotent,
         final boolean forceNew,
         final Object... initArgs){
      this.readOptimization = readOptimization && Reflection.hasReadOnlyMethods(clazz);
      this.isIdempotent = isIdempotent;
      this.forceNew = forceNew;
      this.initArgs = initArgs;
   }

   public final Object getProxy(){
      return proxy;
   }

   public abstract Reference getReference();

   public abstract void doExecute(Call call);

   protected Object execute(Call call)
         throws Throwable {

      if (log.isTraceEnabled())
         log.trace(this + " Executing "+call);

      CompletableFuture<CallResponse> future = new CompletableFuture<CallResponse>();

      registeredCalls.put(call.getCallID(), future);

      CallResponse response = null;
      Object ret = null;
      int attempts = 0;
      while(!future.isDone()) {
         try {
            attempts++;
            doExecute(call);
            response = future.get(TTIMEOUT_TIME, TimeUnit.MILLISECONDS);
            ret = response.getResult();
//            if (ret instanceof Throwable)
//               throw new ExecutionException((Throwable) ret);
         }catch (TimeoutException e) {
            if (!future.isDone())
               log.warn(" Failed "+ call + " ("+e.getMessage()+")");
            if (attempts==MAX_ATTEMPTS) {
               registeredCalls.remove(call.getCallID());
               throw new TimeoutException(call + " failed");
            }
            Thread.sleep(TTIMEOUT_TIME);
         }
         if (ret instanceof Throwable)
            throw (Throwable) ret;

      }

      registeredCalls.remove(call.getCallID());

      if (readOptimization && response.getState()!=null ) {
         this.state = response.getState();
      }

      if (log.isTraceEnabled())
         log.trace(this + " Returning " + ret);

      return ret;

   }


   protected static void handleFuture(CallResponse response){
      try {

         if (!registeredCalls.containsKey(response.getCallID())) {
            log.trace("Future " + response.getCallID() + " ignored");
            return; // duplicate received
         }

         CompletableFuture future = registeredCalls.get(response.getCallID());

         assert (future!=null);

         registeredCalls.remove(response.getCallID());

         future.complete(response);

      } catch (Exception e) {
         e.printStackTrace();
      }

   }

}
