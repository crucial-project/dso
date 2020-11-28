package org.crucial.dso.client;

import org.crucial.dso.*;

import java.util.List;
import java.util.Map;

/**
 * An interface to shared objects without resorting to AspectJ.
 *
 * @author Daniel, Pierre
 */
public class Client {

    private static final String DSO = "DSO";
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

    public synchronized static Client getClient() {
        String server = System.getenv(DSO);
        return getClient(server, 0);
    }

    public Logger getLog(String name) {
        return factory.getInstanceOf(Logger.class, name);
    }

    public <V> List<V> getAtomicList(String name) {
        return factory.getInstanceOf(AtomicList.class, name);
    }

    public <T> Future<T> getFuture(String name, boolean forceNew) {
        return forceNew ? factory.getInstanceOf(Future.class, name)
                : factory.getInstanceOf(Future.class, name, false, false, false);
    }

    public MonitorCyclicBarrier getMonitorCyclicBarrier(String name, int parties) {
        return factory.getInstanceOf(MonitorCyclicBarrier.class, name, false, false, false, name, parties);
    }

    public CyclicBarrier getCyclicBarrier(String name, int parties) {
        AtomicCounter counter = client.getAtomicCounter(name+"-counter",0);
        AtomicCounter generation = client.getAtomicCounter(name+"-generation",0);
        return new CyclicBarrier(name, parties, counter, generation);
    }

    public ScalableCyclicBarrier getScalableCyclicBarrier(String name, int parties) {
        int logParties = (int)(Math.log(parties)/Math.log(2));
       AtomicBoolean[][] answers = new AtomicBoolean[parties][logParties];
        for(int p=0; p<parties; p++) {
            for(int i=0; i<logParties; i++){
                answers[p][i] = client.getAtomicBoolean(name+"-"+p+"-"+i,false);
            }
        }
        AtomicCounter identity = client.getAtomicCounter(name+"-identity",-1);
        return new ScalableCyclicBarrier(name, parties, answers, identity);
    }

    public Semaphore getSemaphore(String name) {
        return factory.getInstanceOf(Semaphore.class, name);
    }

    public Semaphore getSemaphore(String name, int permits) {
        return factory.getInstanceOf(Semaphore.class, name, false, false, false, name, permits);
    }

    public AtomicInteger getAtomicInt(String name) {
        return factory.getInstanceOf(AtomicInteger.class, name);
    }

    public AtomicInteger getAtomicInt(String name, int initialValue) {
        return factory.getInstanceOf(AtomicInteger.class, name, false, false, false, name, initialValue);
    }

    public AtomicLong getAtomicLong(String name) {
        return factory.getInstanceOf(AtomicLong.class, name);
    }

    public AtomicByteArray getAtomicByteArray(String name) {
        return factory.getInstanceOf(AtomicByteArray.class, name);
    }

    public AtomicBoolean getAtomicBoolean(String name, boolean initialValue) {
        return factory.getInstanceOf(AtomicBoolean.class, name, false, false, false, name, initialValue);
    }

    public AtomicBoolean getAtomicBoolean(String name) {
        return factory.getInstanceOf(AtomicBoolean.class, name);
    }

    public AtomicCounter getAtomicCounter(String name, int initialValue) {
        return factory.getInstanceOf(AtomicCounter.class, name, false, false, false, name, initialValue);
    }

    public Map getMap(){
        return client.factory.getCache();
    }

    public AtomicMap getAtomicMap(String name) {
        return factory.getInstanceOf(AtomicMap.class, name);
    }

    public AtomicMatrix getAtomicMatrix(String name, Class clazz, int n, int m) {
        return factory.getInstanceOf(AtomicMatrix.class, name, false, false, false, name, clazz, n, m);
    }

    public Blob getAtomicBlob(String name) {
        return factory.getInstanceOf(Blob.class, name, false, false, false);
    }

    public CountDownLatch getCountDownLatch(String name, int parties) {
        AtomicCounter counter = client.getAtomicCounter(name, parties);
        return new CountDownLatch(name, parties, counter);
    }

    public void clear() {
        factory.clear();
    }

    public void close() {
        factory.close();
    }

}
