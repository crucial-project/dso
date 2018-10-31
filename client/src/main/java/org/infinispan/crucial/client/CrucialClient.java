package org.infinispan.crucial.client;

import org.infinispan.crucial.CAtomicInt;
import org.infinispan.crucial.CFuture;
import org.infinispan.crucial.Factory;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * CRUCIAL client to get instances of basic objects.
 *
 * @author Daniel
 */
public class CrucialClient{
    public static final String defaultServer = "127.0.0.1:11222";
    static CrucialClient client;
    Factory factory;


    private CrucialClient(String server){
        factory = Factory.get(server);
    }

    public static CrucialClient getClient(String server){
        if (client == null) {
            client = new CrucialClient(server);
        }
        return client;
    }

    public CrucialClient getClient(){
        return getClient(defaultServer);
    }

    public <K, V> Map<K, V> getMap(String key){
        return factory.getInstanceOf(Map.class, key);
    }

    public <T> Future<T> getFuture(String key){
        return new CFuture<>(key);
    }

    public CAtomicInt getAtomicInt(String key){
        return factory.getInstanceOf(CAtomicInt.class, key);
    }
}
