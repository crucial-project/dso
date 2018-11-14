package org.infinispan.crucial.client;

import org.infinispan.crucial.CAtomicByteArray;
import org.infinispan.crucial.CAtomicInt;
import org.infinispan.crucial.CAtomicLong;
import org.infinispan.crucial.CFuture;
import org.infinispan.crucial.Factory;

import java.util.Map;

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

    public static CrucialClient getClient(){
        return getClient(defaultServer);
    }

    public <K, V> Map<K, V> getMap(String key){
        return factory.getInstanceOf(Map.class, key);
    }

    public <T> CFuture<T> getFuture(String key){
        return new CFuture<>(key);
    }

    public CAtomicInt getAtomicInt(String key){
        return factory.getInstanceOf(CAtomicInt.class, key);
    }

    public CAtomicLong getAtomicLong(String key){
        return factory.getInstanceOf(CAtomicLong.class, key);
    }
    public CAtomicByteArray getAtomicByteArray(String key){
        return factory.getInstanceOf(CAtomicByteArray.class, key);
    }
}
