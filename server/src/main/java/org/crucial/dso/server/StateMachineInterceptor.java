package org.crucial.dso.server;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.crucial.dso.utils.Context;
import org.crucial.dso.utils.ContextManager;
import org.crucial.dso.utils.Reflection;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.context.InvocationContext;
import org.crucial.dso.Factory;
import org.crucial.dso.object.Call;
import org.crucial.dso.object.CallConstruct;
import org.crucial.dso.object.CallInvoke;
import org.crucial.dso.object.CallResponse;
import org.crucial.dso.object.Reference;
import org.infinispan.interceptors.impl.ClusteringInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Random;

public class StateMachineInterceptor extends ClusteringInterceptor {

    private static final Log log = LogFactory.getLog(StateMachineInterceptor.class);

    private Factory factory;
    private CallResponseCache responseCache = new CallResponseCache();
    private boolean withIdempotence;

    @Override
    public java.lang.Object visitClearCommand(InvocationContext ctx, ClearCommand command) throws Throwable {

        log.info(" Clearing all objects");

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

        if (withIdempotence && responseCache.contains(call)) {

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
                            result = Reflection.callObject(object, invocation.method, args);
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
                        if (withIdempotence) responseCache.clear(call);

                    }

                    response.setResult(null);

                }

            } catch (Exception e) {
                throw e;
            }

        } // end compute return value

        // save return value
        if (withIdempotence) responseCache.put(call,response);

        command.setValue(object);
        invokeNext(ctx, command);

        if (log.isDebugEnabled()) {
            log.debug("" + call.toString() + " = "+response.getResult());
        }

        return response;
    }

    public void setup(Factory factory, boolean useIdempotence){
        this.factory = factory;
        this.withIdempotence = useIdempotence;
    }

    // utils

    @Override
    protected Log getLog() {
        return log;
    }
}
