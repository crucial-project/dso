package org.infinispan.creson.server;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.InvocationContext;
import org.infinispan.creson.Factory;
import org.infinispan.creson.object.Call;
import org.infinispan.creson.object.CallConstruct;
import org.infinispan.creson.object.CallFuture;
import org.infinispan.creson.object.CallInvoke;
import org.infinispan.creson.object.Reference;
import org.infinispan.creson.utils.Identities;
import org.infinispan.creson.utils.Reflection;
import org.infinispan.distribution.DistributionInfo;
import org.infinispan.interceptors.distribution.NonTxDistributionInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.infinispan.creson.server.Marshalling.marshall;
import static org.infinispan.creson.server.Marshalling.unmarshall;
import static org.infinispan.creson.utils.Reflection.callObject;
import static org.infinispan.creson.utils.Reflection.hasReadOnlyMethods;

public class StateMachineInterceptor extends NonTxDistributionInterceptor {

    // Class fields & methods
    private static final Log log = LogFactory.getLog(StateMachineInterceptor.class);

    private ConcurrentMap<Reference, CallFuture> lastCall = new ConcurrentHashMap<>();
    private Factory factory;

    @Override
    public java.lang.Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {

        if (!(command.getValue() instanceof Call)) {
            return handleDefault(ctx, command);
        }

        assert (command.getKey() instanceof Reference) & (command.getValue() instanceof Call);

        if (log.isTraceEnabled()) {
            DistributionInfo info = dm.getCacheTopology().getDistribution(command.getKey());
            System.out.println(info.isPrimary() + " " + info.isWriteBackup() + " " + command);
        }

        Reference reference = (Reference) command.getKey();
        if (log.isTraceEnabled())
            log.trace(" Accessing " + reference);

        Call call = (Call) command.getValue();
        CallFuture future;

        // FIXME elasticity

        CacheEntry<Reference, Object> entry = ctx.lookupEntry(reference);
        assert (call instanceof CallConstruct) | (entry.getValue()!=null);

        if (log.isTraceEnabled())
            log.trace(" Received [" + call + "] " +
                    "(completed=" + lastCall.get(reference).equals(call) + ", " + reference + ")");

        future = new CallFuture(call);

        if (lastCall.containsKey(reference)
                && lastCall.get(reference).equals(future)) {

            // call already completed
            future = lastCall.get(reference);

        } else {

            try {

                if (call instanceof CallInvoke) {

                    CallInvoke invocation = (CallInvoke) call;

                    assert (entry.getValue() != null);

                    java.lang.Object[] args = Reference.unreference(
                            invocation.arguments,
                            factory);

                    RandomBasedGenerator generator = new RandomBasedGenerator(
                            new Random(call.getCallID().getLeastSignificantBits()
                                    + call.getCallID().getMostSignificantBits()));

                    java.lang.Object response;

                    try {

                         if (log.isTraceEnabled())
                             log.trace(dm.getCacheTopology().getLocalAddress()+"#"+call);

                         Identities.setThreadLocal(generator);
                         synchronized (entry.getValue()) { // synchronization contract
                            response = callObject(entry.getValue(), invocation.method, args);
                         }
                         Identities.unsetThreadLocal();

                        future.set(response);
                        if (log.isTraceEnabled())
                            log.trace(dm.getCacheTopology().getLocalAddress()+"#"+future + " @" + entry.getValue());

                    } catch (Throwable e) {
                        future.set(e);
                    }


                } else if (call instanceof CallConstruct) {

                    CallConstruct callConstruct = (CallConstruct) call;

                    if (entry.getValue() == null | callConstruct.getForceNew()) {

                        if (log.isTraceEnabled())
                            log.trace(" New [" + reference + "]");

                        entry.setValue(
                                Reflection.open(reference, callConstruct.getInitArgs(),factory));
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
            synchronized (entry.getValue()) { // synchronization contract
                future.setState(unmarshall(marshall(entry.getValue())));
            }
        }

        PutKeyValueCommand clone = cf.buildPutKeyValueCommand(
                command.getKey(), entry.getValue(),
                command.getMetadata(), command.getFlagsBitSet());
        invokeNext(ctx, clone);

        return future;
    }

    public void setup(Factory factory) {
        this.factory = factory;
    }

    // utils

    @Override
    protected Log getLog() {
        return log;
    }
}
