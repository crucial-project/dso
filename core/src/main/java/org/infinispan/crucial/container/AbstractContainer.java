package org.infinispan.crucial.container;

import javassist.util.proxy.MethodFilter;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.crucial.object.Call;
import org.infinispan.crucial.object.CallResponse;
import org.infinispan.crucial.object.Reference;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.infinispan.crucial.utils.Reflection.hasReadOnlyMethods;

/**
 * Uninstalling a listener is not necessary, as when all clients disconnect,
 * it is automatically removed.
 *
 * @author Pierre Sutra
 */
public abstract class AbstractContainer{

    public static final int TTIMEOUT_TIME = 1000;
    public static final int MAX_ATTEMPTS = 3;
    protected static final Map<UUID, CallResponse> registeredCalls = new ConcurrentHashMap<>();
    protected static final Log log = LogFactory.getLog(AbstractContainer.class);
    protected static final MethodFilter methodFilter = m -> ! m.getName().equals("finalize");

    protected boolean readOptimization;
    protected Object proxy;
    protected Object state;
    protected boolean forceNew;
    protected Object[] initArgs;

    public AbstractContainer(
            Class clazz,
            final boolean readOptimization,
            final boolean forceNew,
            final Object... initArgs){
        this.readOptimization = readOptimization && hasReadOnlyMethods(clazz);
        this.forceNew = forceNew;
        this.initArgs = initArgs;
    }

    protected static void handleFuture(CallResponse future){
        try {
            assert (future instanceof CallResponse && future.isDone());
            if (! registeredCalls.containsKey(future.getCallID())) {
                log.trace("Future " + future.getCallID() + " ignored");
                return; // duplicate received
            }
            CallResponse clientFuture = registeredCalls.get(future.getCallID());
            assert (clientFuture != null);
            registeredCalls.remove(future.getCallID());
            clientFuture.setState(future.getState());
            clientFuture.set(future.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public final Object getProxy(){
        return proxy;
    }

    public abstract Reference getReference();

    public abstract void doExecute(Call call);

    protected Object execute(Call call)
            throws Throwable{

        if (log.isTraceEnabled())
            log.trace(this + " Executing " + call);

        CallResponse future = new CallResponse(getReference(), call);

        registeredCalls.put(call.getCallID(), future);

        Object ret = null;
        int attempts = 0;
        while (! future.isDone()) {
            try {
                attempts++;
                doExecute(call);
                ret = future.get(TTIMEOUT_TIME, TimeUnit.MILLISECONDS);
//            if (ret instanceof Throwable)
//               throw new ExecutionException((Throwable) ret);
            } catch (TimeoutException e) {
                if (! future.isDone())
                    log.warn(" Failed " + call + " (" + e.getMessage() + ")");
                if (attempts == MAX_ATTEMPTS) {
                    registeredCalls.remove(call.getCallID());
                    throw new TimeoutException(call + " failed");
                }
                Thread.sleep(TTIMEOUT_TIME);
            }
            if (ret instanceof Throwable)
                throw ((Throwable) ret).getCause();

        }

        registeredCalls.remove(call.getCallID());

        if (readOptimization && future.getState() != null) {
            this.state = future.getState();
        }

        if (log.isTraceEnabled())
            log.trace(this + " Returning " + ret);

        return ret;

    }

}
