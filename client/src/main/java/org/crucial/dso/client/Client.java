package org.crucial.dso.client;

import org.crucial.dso.*;

import java.util.ArrayList;
import java.util.List;
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

    private Client(String server, long seed) { factory = Factory.get(server, seed); }

    /**
     * Return a client or create one if not already present.
     * @param server
     * @param seed
     * @return
     */
    public synchronized static Client getClient(String server, long seed) {
        if (client == null) {
            client = new Client(server== null ? defaultServer : server, seed);
        }
        return client;
    }

    public synchronized static Client getClient(String server) {
        return getClient(server, 0);
    }

    public Logger getLog(String key) {
        return factory.getInstanceOf(Logger.class, key);
    }

    public <V> List<V> getAtomicList(String key) {
        return factory.getInstanceOf(AtomicList.class, key);
    }

    public <T> Future<T> getFuture(String key, boolean forceNew) {
        return forceNew ? factory.getInstanceOf(Future.class, key)
                : factory.getInstanceOf(Future.class, key, false, false, false);
    }

    public MonitorCyclicBarrier getMonitorCyclicBarrier(String key, int parties) {
        return factory.getInstanceOf(MonitorCyclicBarrier.class, key, false, false, false, key, parties);
    }

    public CyclicBarrier getCyclicBarrier(String key, int parties) {
        AtomicCounter counter = client.getAtomicCounter(key+"-counter",0);
        AtomicCounter generation = client.getAtomicCounter(key+"-generation",0);
        return new CyclicBarrier(key, parties, counter, generation);
    }

    public ScalableCyclicBarrier getScalableCyclicBarrier(String key, int parties) {
        int logParties = (int)(Math.log(parties)/Math.log(2));
       AtomicBoolean[][] answers = new AtomicBoolean[parties][logParties];
        for(int p=0; p<parties; p++) {
            for(int i=0; i<logParties; i++){
                answers[p][i] = client.getAtomicBoolean(key+"-"+p+"-"+i,false);
            }
        }
        AtomicCounter identity = client.getAtomicCounter(key+"-identity",-1);
        return new ScalableCyclicBarrier(key, parties, answers, identity);
    }

    public Semaphore getSemaphore(String key) {
        return factory.getInstanceOf(Semaphore.class, key);
    }

    public Semaphore getSemaphore(String key, int permits) {
        return factory.getInstanceOf(Semaphore.class, key, false, false, false, key, permits);
    }

    public AtomicInteger getAtomicInt(String key) {
        return factory.getInstanceOf(AtomicInteger.class, key);
    }

    public AtomicInteger getAtomicInt(String key, int initialValue) {
        return factory.getInstanceOf(AtomicInteger.class, key, false, false, false, key, initialValue);
    }

    public AtomicLong getAtomicLong(String key) {
        return factory.getInstanceOf(AtomicLong.class, key);
    }

    public AtomicByteArray getAtomicByteArray(String key) {
        return factory.getInstanceOf(AtomicByteArray.class, key);
    }

    public AtomicBoolean getAtomicBoolean(String key, boolean initialValue) {
        return factory.getInstanceOf(AtomicBoolean.class, key, false, false, false, key, initialValue);
    }

    public AtomicBoolean getAtomicBoolean(String key) {
        return factory.getInstanceOf(AtomicBoolean.class, key);
    }

    public AtomicCounter getAtomicCounter(String key, int initialValue) {
        return factory.getInstanceOf(AtomicCounter.class, key, false, false, false, key, initialValue);
    }

    public Map getMap(){
        return client.factory.getCache();
    }

    public AtomicMap getAtomicMap(String key) {
        return factory.getInstanceOf(AtomicMap.class, key);
    }

    public AtomicMatrix getAtomicMatrix(String key, Class clazz, int n, int m) {
        return factory.getInstanceOf(AtomicMatrix.class, key, false, false, false, key, clazz, n, m);
    }

    public Blob getBlob(String key) {
        return factory.getInstanceOf(Blob.class, key, false, false, false);
    }

    public CountDownLatch getCountDownLatch(String key, int parties) {
        AtomicCounter counter = client.getAtomicCounter(key, parties);
        return new CountDownLatch(key, parties, counter);
    }

    public void clear() {
        factory.clear();
    }

    public void close() {
        factory.close();
    }

}
