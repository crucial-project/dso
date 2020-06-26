package org.crucial.dso.client;

import org.crucial.dso.*;

import java.util.ArrayList;
import java.util.Map;

/**
 * An interface to shared objects without resorting to AspectJ.
 *
 * @author Daniel, Pierre
 */
public class Client {

    private static final String defaultServer = "127.0.0.1:11222";
    private static Client client;
    private Factory factory;

    private Client(String server) {
        factory = Factory.get(server);
    }

    /**
     * Return a client or create one if not already present.
     * @param server
     * @return
     */
    public synchronized static Client getClient(String server) {
        if (client == null) {
            client = new Client(server== null ? defaultServer : server);
        }
        return client;
    }

    public Logger getLog(String key) {
        return factory.getInstanceOf(Logger.class, key);
    }

    public <K, V> Map<K, V> getMap(String key) {
        return factory.getInstanceOf(Map.class, key);
    }

    public <T> ArrayList<T> getArrayList(String key) {
        return factory.getInstanceOf(ArrayList.class, key);
    }

    public <T> ArrayList<T> getArrayList(String key, int initialCapacity) {
        return factory.getInstanceOf(ArrayList.class, key);
    }

    public <T> Future<T> getCleanFuture(String key, boolean forceNew) {
        return forceNew ? factory.getInstanceOf(Future.class, key)
                : factory.getInstanceOf(Future.class, key, false, false, true);
    }

    public MonitorCyclicBarrier getMonitorCyclicBarrier(String key) {
        return factory.getInstanceOf(MonitorCyclicBarrier.class, key);
    }

    public MonitorCyclicBarrier getMonitorCyclicBarrier(String key, int parties) {
        return factory.getInstanceOf(MonitorCyclicBarrier.class, key, false, false, true);
    }

    public Semaphore getSemaphore(String key) {
        return factory.getInstanceOf(Semaphore.class, key);
    }

    public Semaphore getSemaphore(String key, int permits) {
        return factory.getInstanceOf(Semaphore.class, key, false, false, true, permits);
    }

    public AtomicInteger getAtomicInt(String key) {
        return factory.getInstanceOf(AtomicInteger.class, key);
    }

    public AtomicInteger getAtomicInt(String key, int initialValue) {
        return factory.getInstanceOf(AtomicInteger.class, key, false, false, true, initialValue);
    }

    public AtomicLong getAtomicLong(String key) {
        return factory.getInstanceOf(AtomicLong.class, key);
    }

    public AtomicByteArray getAtomicByteArray(String key) {
        return factory.getInstanceOf(AtomicByteArray.class, key);
    }

    public AtomicBoolean getBoolean(String key, boolean initialValue) {
        return factory.getInstanceOf(AtomicBoolean.class, key, false, false, true, initialValue);
    }

    public AtomicBoolean getBoolean(String key) {
        return factory.getInstanceOf(AtomicBoolean.class, key);
    }

}

