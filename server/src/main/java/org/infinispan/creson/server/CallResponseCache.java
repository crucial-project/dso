package org.infinispan.creson.server;

import org.infinispan.creson.object.Call;
import org.infinispan.creson.object.CallResponse;
import org.infinispan.creson.object.Reference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class CallResponseCache {

    private ConcurrentMap<Reference,ConcurrentHashMap<UUID, Map<UUID,Object>>> responses = new ConcurrentHashMap<>();

    public boolean contains(Call call) {
        return responses.containsKey(call.getReference())
                && responses.get(call.getReference()).containsKey(call.getCallerID())
                && responses.get(call.getReference()).get(call.getCallerID()).containsKey(call.getCallID());
    }

    public CallResponse get(Call call){
        CallResponse response = null;
        if (responses.containsKey(call.getReference())) {
            if (responses.get(call.getReference()).containsKey(call.getCallerID())) {
                if (responses.get(call.getReference()).get(call.getCallerID()).containsKey(call.getCallID())) {
                    response = new CallResponse(call.getReference(), call);
                    response.setResult(responses.get(call.getReference()).get(call.getCallerID()).get(call.getCallID()));
                }
            }
        }
        return response;
    }

    public void put(Call call, CallResponse response) {
        if (!responses.containsKey(call.getReference()))
            responses.putIfAbsent(call.getReference(), new ConcurrentHashMap<>());

        if (!responses.get(call.getReference()).containsKey(call.getCallerID()))
            responses.get(call.getReference()).put(call.getCallerID(), new HashMap<>());

        responses.get(call.getReference()).get(call.getCallerID()).put(
                call.getCallID(),
                response.getResult());
    }

    /**
     * Assumed to be sequentially executed.
     */
    public void clearAll(){
        responses.clear();
    }

   public void clear(Call call){
        if (responses.containsKey(call.getReference())) {
            responses.get(call.getReference()).clear();
        }
    }
}
