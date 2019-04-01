package org.infinispan.creson.server;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.creson.Factory;
import org.infinispan.creson.object.Call;
import org.infinispan.creson.object.CallConstruct;
import org.infinispan.creson.object.CallInvoke;
import org.infinispan.creson.object.CallResponse;
import org.infinispan.creson.object.Reference;
import org.infinispan.creson.utils.Context;
import org.infinispan.creson.utils.ContextManager;
import org.infinispan.creson.utils.Reflection;
import org.infinispan.interceptors.distribution.NonTxDistributionInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.infinispan.creson.server.Marshalling.marshall;
import static org.infinispan.creson.server.Marshalling.unmarshall;
import static org.infinispan.creson.utils.Reflection.callObject;
import static org.infinispan.creson.utils.Reflection.hasReadOnlyMethods;

public class StateMachineInterceptor extends NonTxDistributionInterceptor {

    private static final Log log = LogFactory.getLog(StateMachineInterceptor.class);

    private ConcurrentMap<Reference,Map<UUID,CallResponse>> lastCall = new ConcurrentHashMap<>(); // UUID == callID
    private Factory factory;

    @Override
    public java.lang.Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {

        if (!(command.getValue() instanceof Call)) {
            return handleDefault(ctx, command);
        }

        assert (command.getKey() instanceof Reference) & (command.getValue() instanceof Call);

        Call call = (Call) command.getValue();

        Reference reference = call.getReference();
        if (log.isTraceEnabled())
            log.trace(" Accessing " + reference);

        CallResponse future;
        Object object = ctx.lookupEntry(reference).getValue();

        // FIXME elasticity
        // assert (call instanceof CallConstruct) | (object!=null);

        if (log.isTraceEnabled()) {
            log.trace(" Received [" + call.toString() + "]");
            log.trace(" With ID " + call.getCallID() + "]");
            log.trace(" By " + call.getCallerID() + "]");
            log.trace(" Object "+object);
            // log.trace(" lastCall="+lastCall);
        }

        future = new CallResponse(reference ,call);

        if (!lastCall.containsKey(reference)){
            lastCall.put(reference,new HashMap<>());
        }

        if (lastCall.get(reference).containsKey(call.getCallID())) {

            if (log.isTraceEnabled()) {
                log.trace(" already completed");
            }

            // call already completed
            future = lastCall.get(reference).get(call.getCallID());

        } else {

            try {

                ContextManager.set(
                        new Context(
                                call.getCallerID(),
                                new RandomBasedGenerator(
                                        new Random(call.getCallID().getLeastSignificantBits()
                                                + call.getCallID().getMostSignificantBits())),
                                factory));

                if (call instanceof CallInvoke) {

                    CallInvoke invocation = (CallInvoke) call;

                    // FIXME elasticity
                    // assert (object != null);
                    if (object == null ) {
                        object = Reflection.open(reference, new Object[0]);
                    }

                    java.lang.Object[] args = invocation.arguments;

                    java.lang.Object response;

                    try {

                        synchronized (object) { // synchronization contract
                            response = callObject(object, invocation.method, args);
                        }

                        future.set(response);

                    } catch (Throwable e) {
                        future.set(e);
                    }


                } else if (call instanceof CallConstruct) {

                    CallConstruct callConstruct = (CallConstruct) call;

                    if (object == null | callConstruct.getForceNew()) {

                        if (log.isTraceEnabled())
                            log.trace(" New [" + reference + "]");

                        object = Reflection.open(reference, callConstruct.getInitArgs());

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
        lastCall.get(reference).put(call.getCallID(),future);

        // save state if required
        if (hasReadOnlyMethods(reference.getClazz())) { // FIXME state = byte array
            synchronized (object) { // synchronization contract
                future.setState(unmarshall(marshall(object)));
            }
        }

        PutKeyValueCommand clone = cf.buildPutKeyValueCommand(
                command.getKey(),
                object,
                command.getSegment(),
                command.getMetadata(),
                command.getFlagsBitSet());
        invokeNext(ctx, clone);

        if (log.isTraceEnabled()) {
            log.trace(" Executed [" + call.toString() + "] -> "+future.toString());
        }

        return future;
    }

    public void setup(Factory factory){
        this.factory = factory;
    }

    // utils

    @Override
    protected Log getLog() {
        return log;
    }
}
