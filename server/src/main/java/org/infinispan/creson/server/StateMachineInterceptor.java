package org.infinispan.creson.server;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.infinispan.commands.write.ClearCommand;
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
import org.infinispan.interceptors.impl.ClusteringInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Random;

import static org.infinispan.creson.server.Marshalling.marshall;
import static org.infinispan.creson.server.Marshalling.unmarshall;
import static org.infinispan.creson.utils.Reflection.callObject;
import static org.infinispan.creson.utils.Reflection.hasReadOnlyMethods;

public class StateMachineInterceptor extends ClusteringInterceptor {

    private static final Log log = LogFactory.getLog(StateMachineInterceptor.class);

    private Factory factory;
    private CallResponseCache responseCache = new CallResponseCache();

    @Override
    public java.lang.Object visitClearCommand(InvocationContext ctx, ClearCommand command) throws Throwable {

        log.trace(" Clearing all");

        responseCache.clearAll();
        return super.visitClearCommand(ctx, command);
    }

    @Override
    public java.lang.Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {

        if (!(command.getValue() instanceof Call)) {
            return handleDefault(ctx, command);
        }

        assert (command.getKey() instanceof Reference) & (command.getValue() instanceof Call);

        Call call = (Call) command.getValue();

        Reference reference = call.getReference();
        CallResponse response;
        Object object = ctx.lookupEntry(reference).getValue();

        // FIXME elasticity
        // assert (call instanceof CallConstruct) | (object!=null);

        if (log.isTraceEnabled()) {
            log.trace(" Rcv [" + call.toString() + ", key=" + reference+", call=" + call.getCallID() + "["+responseCache.contains(call)+"], caller=" + call.getCallerID() + "]");
        }

        response = new CallResponse(reference ,call);

        if (responseCache.contains(call)) {

            return responseCache.get(call);

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

                    java.lang.Object result;

                    try {

                        synchronized (object) { // synchronization contract
                            result = callObject(object, invocation.method, args);
                        }

                        response.setResult(result);

                    } catch (Throwable e) {
                        response.setResult(e);
                    }


                } else if (call instanceof CallConstruct) {

                    CallConstruct callConstruct = (CallConstruct) call;

                    if (object == null | callConstruct.getForceNew()) {

                        if (log.isTraceEnabled())
                            log.trace(" New [" + reference + "]");

                        object = Reflection.open(reference, callConstruct.getInitArgs());
                        responseCache.clear(call);

                    }

                    response.setResult(null);

                }

            } catch (Exception e) {
                throw e;
            }

        } // end compute return value

        // save return value
        responseCache.put(call,response);

        // save state if required
        if (hasReadOnlyMethods(reference.getClazz())) { // FIXME state = byte array
            synchronized (object) { // synchronization contract
                byte[] buf = marshall(object);
                response.setState(unmarshall(buf));
                if (log.isTraceEnabled()) {
                    log.trace(" keeping state "+buf.length+"B");
                }
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
            log.trace(" Executed [" + call.toString() + "] = "+response.toString());
        }

        return response;
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
