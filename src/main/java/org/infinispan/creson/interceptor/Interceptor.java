package org.infinispan.creson.interceptor;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.google.common.cache.CacheBuilder;
import org.infinispan.Cache;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.creson.StaticEntity;
import org.infinispan.creson.object.*;
import org.infinispan.creson.utils.ThreadLocalUUIDGenerator;
import org.infinispan.distribution.DistributionInfo;
import org.infinispan.interceptors.distribution.NonTxDistributionInterceptor;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.persistence.Id;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.infinispan.creson.CresonModuleLifeCycle.CRESON_CACHE_NAME;
import static org.infinispan.creson.object.Utils.*;

public class Interceptor extends NonTxDistributionInterceptor {

    // Class fields & methods
    private static final Log log = LogFactory.getLog(Interceptor.class);
    private static final long MAX_COMPLETED_CALLS = 1000; // around 1s at max throughput, not much... and already too much

    private ConcurrentMap<Reference,Map<Call, CallFuture>> completedCalls = new ConcurrentHashMap<>();
    private EmbeddedCacheManager cacheManager;
    private Cache cache;

    public Interceptor(EmbeddedCacheManager cacheManager){
        this.cacheManager = cacheManager;
    }

    @Override
    public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {

        if (cache==null)
            cache = cacheManager.getCache(CRESON_CACHE_NAME);

        if (log.isTraceEnabled()) {
            DistributionInfo info = cache.getAdvancedCache().getDistributionManager().getCacheTopology().getDistribution(command.getKey());
            System.out.println(info.isPrimary() + " " + info.isWriteBackup() + " " + command);
        }

        if (!(command.getValue() instanceof Call)) {
            return handleDefault(ctx,command);
        }

        assert (command.getKey() instanceof Reference) & (command.getValue() instanceof Call);

        Reference reference = (Reference) command.getKey();
        Call call = (Call) command.getValue();
        Object object = null;
        if (ctx.lookupEntry(reference).getValue()!=null) {
            object = ctx.lookupEntry(reference).getValue();
        }
        CallFuture future;

        if (log.isTraceEnabled())
            log.trace(" Accessing " + reference);

        if (!completedCalls.containsKey(reference)) {
            completedCalls.put(reference,
                    (ConcurrentMap) CacheBuilder.newBuilder()
                            .maximumSize(MAX_COMPLETED_CALLS)
                            .build().asMap());
        }

        synchronized (completedCalls.get(reference)) {

            if (log.isTraceEnabled())
                log.trace(" Received [" + call + "] (completed="
                        + completedCalls.containsKey(call) + ", " + reference + ")");

            future = new CallFuture(call.getCallID(), call.getListenerID());

            if (completedCalls.get(reference).containsKey(call)) {

                // call already completed
                future = completedCalls.get(reference).get(call);

            } else {

                try {

                    RandomBasedGenerator generator = new RandomBasedGenerator(
                                        new Random(call.getCallID().getLeastSignificantBits()
                                                + call.getCallID().getMostSignificantBits()));

                    if (call instanceof CallInvoke) {

                        CallInvoke invocation = (CallInvoke) call;

                        if (object==null) {
                            log.error("object was re-created!");
                            object = createObejct(reference, new Object[]{});
                        }

                        Object[] args = Reference.unreference(
                                invocation.arguments,
                                cache);

                        ThreadLocalUUIDGenerator.setThreadLocal(generator);

                        try {
                            Object response =
                                    Utils.callObject(
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

                        ThreadLocalUUIDGenerator.unsetThreadLocal();

                    } else if (call instanceof CallOpen) {

                        CallOpen callOpen = (CallOpen) call;

                        assert !cache.containsKey(reference) || cache.get(reference) != null;

                        if (callOpen.getForceNew() | object == null ) {

                            if (log.isTraceEnabled())
                                log.trace(" New [" + reference + "]");

                            object = createObejct(reference, callOpen.getInitArgs());

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
            completedCalls.get(reference).put(call, future);

            // save state if required
            if (hasReadOnlyMethods(reference.getClazz())) {
                future.setState(unmarshall(marshall(object)));
            }

        } // synchronized(reference)

        PutKeyValueCommand clone = cf.buildPutKeyValueCommand(
                command.getKey(),object,
                command.getMetadata(),command.getFlagsBitSet());
        clone.setValue(object);
        invokeNext(ctx, clone);

        return future;
    }

    // utils

    private Object createObejct(Reference reference, Object[] initArgs)
            throws IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException, NoSuchFieldException {

        Object ret = Utils.initObject(
                reference.getClazz(),Reference.unreference(initArgs,cache));

        // force the key field, in case it is created per default
        if (reference.getClazz().getAnnotation(StaticEntity.class)!=null) {
            java.lang.reflect.Field field = null;
            for (java.lang.reflect.Field f : reference.getClazz().getFields()) {
                if (f.getAnnotation(Id.class) != null) {
                    field = f;
                    break;
                }
            }
            field.set(ret, reference.getKey());
        }

        return ret;

    }


    @Override
    protected Log getLog() {
        return log;
    }
}
