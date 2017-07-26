package org.infinispan.creson.server;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.infinispan.Cache;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.creson.Factory;
import org.infinispan.creson.object.Call;
import org.infinispan.creson.object.CallConstruct;
import org.infinispan.creson.object.CallFuture;
import org.infinispan.creson.object.CallInvoke;
import org.infinispan.creson.object.Reference;
import org.infinispan.creson.utils.Reflection;
import org.infinispan.creson.utils.ThreadLocalUUIDGenerator;
import org.infinispan.distribution.DistributionInfo;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.interceptors.distribution.NonTxDistributionInterceptor;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;
import static org.infinispan.creson.server.Marshalling.marshall;
import static org.infinispan.creson.server.Marshalling.unmarshall;
import static org.infinispan.creson.utils.Reflection.callObject;
import static org.infinispan.creson.utils.Reflection.hasReadOnlyMethods;

public class  Interceptor extends NonTxDistributionInterceptor {

    // Class fields & methods
    private static final Log log = LogFactory.getLog(Interceptor.class);

    private ConcurrentMap<Reference,CallFuture> lastCall = new ConcurrentHashMap<>();
    private EmbeddedCacheManager cacheManager;
    private DistributionManager distributionManager;
    private Cache cache;

    public Interceptor(EmbeddedCacheManager cacheManager){
        this.cacheManager = cacheManager;
    }

    @Override
    public java.lang.Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {

        if (!(command.getValue() instanceof Call)) {
            return handleDefault(ctx,command);
        }

        assert (command.getKey() instanceof Reference) & (command.getValue() instanceof Call);

        if (cache==null) {
            cache = cacheManager.getCache(CRESON_CACHE_NAME);
            Factory.forCache(cache);
            distributionManager = cache.getAdvancedCache().getDistributionManager();
        }

        if (log.isTraceEnabled()) {
            DistributionInfo info = distributionManager.getCacheTopology().getDistribution(command.getKey());
            System.out.println(info.isPrimary() + " " + info.isWriteBackup() + " " + command);
        }

        Reference reference = (Reference) command.getKey();
        if (log.isTraceEnabled())
            log.trace(" Accessing " + reference);

        Call call = (Call) command.getValue();
        CallFuture future;
        java.lang.Object object;

        synchronized (ctx.lookupEntry(reference)) { // FIXME state transfer can be concurrent

            object = cache.get(reference);

            if (log.isTraceEnabled())
                log.trace(" Received [" + call + "] " +
                        "(completed=" + lastCall.get(reference).equals(call) + ", " + reference + ")");

            future = new CallFuture(call.getCallID());

            if (lastCall.containsKey(reference)
                    && lastCall.get(reference).equals(future)) {

                // call already completed
                future = lastCall.get(reference);

            } else {

                try {

                    if (call instanceof CallInvoke) {

                        CallInvoke invocation = (CallInvoke) call;

                        if (object == null) {
                            log.warn(reference + " re-created " + " @" + cache.getAdvancedCache().getDistributionManager().getCacheTopology().getLocalAddress());
                            object = Reflection.open(reference, new java.lang.Object[]{},cache);
                        }

                        assert object != null;

                        java.lang.Object[] args = Reference.unreference(
                                invocation.arguments,
                                cache);

                        RandomBasedGenerator generator = new RandomBasedGenerator(
                                new Random(call.getCallID().getLeastSignificantBits()
                                        + call.getCallID().getMostSignificantBits()));

                        ThreadLocalUUIDGenerator.setThreadLocal(generator);

                        synchronized (object) { // to enforce atomicity

                            try {

                                java.lang.Object response =
                                        callObject(
                                                object,
                                                invocation.method,
                                                args);

                                future.set(response);

                                if (log.isTraceEnabled())
                                    log.trace(" Called " + invocation + " on " + reference + " (=" +
                                            (response == null ? "null" : response.toString()) + ")");

                            } catch (Throwable e) {
                                future.set(e);
                            }

                        }

                        ThreadLocalUUIDGenerator.unsetThreadLocal();

                    } else if (call instanceof CallConstruct) {

                        CallConstruct callConstruct = (CallConstruct) call;

                        if (callConstruct.getForceNew() | object == null) {

                            if (log.isTraceEnabled())
                                log.trace(" New [" + reference + "]");

                            object = Reflection.open(reference, callConstruct.getInitArgs(), cache);

                        }

                        future.set(null);

                    }

                } catch (Exception e) {
                    throw e;
                }

            } // end compute return value

            assert !future.isCancelled();
            assert future.isDone();

            // save return value
            lastCall.put(reference, future);

            // save state if required
            if (hasReadOnlyMethods(reference.getClazz())) { // FIXME state = byte array
                future.setState(unmarshall(marshall(object)));
            }

            PutKeyValueCommand clone = cf.buildPutKeyValueCommand(
                    command.getKey(), object,
                    command.getMetadata(), command.getFlagsBitSet());
            clone.setValue(object);
            invokeNext(ctx, clone);

        } // synchronized

        return future;
    }

    // utils

    @Override
    protected Log getLog() {
        return log;
    }
}
