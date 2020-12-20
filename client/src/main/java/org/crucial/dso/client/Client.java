package org.crucial.dso.client;

import org.crucial.dso.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A client interface for DSO.
 *
 * @author Daniel, Pierre
 */
public class Client {

    //

    private static Client client;

    @Deprecated
    public synchronized static Client getClient(String server, long seed) {
        if (client == null) {
            client = new Client(server, seed);
        }
        return client;
    }

    @Deprecated
    public synchronized static Client getClient(String server) {
        if (client == null) {
            client = new Client(server, 0);
        }
        return client;
    }

    @Deprecated
    public synchronized static Client getClient() {
        if (client == null) {
            client = new Client();
        }
        return client;
    }

    //

    private Factory factory;

    public Client(){ factory = Factory.get(); }

    public Client(String server) { factory = Factory.get(server); }

    public Client(String server, long seed) { factory = Factory.get(server, seed); }

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
        AtomicCounter counter = getAtomicCounter(name+"-counter",0);
        AtomicCounter generation = getAtomicCounter(name+"-generation",0);
        return new CyclicBarrier(name, parties, counter, generation);
    }

    public ScalableCyclicBarrier getScalableCyclicBarrier(String name, int parties) {
        int logParties = (int)(Math.log(parties)/Math.log(2));
       AtomicBoolean[][] answers = new AtomicBoolean[parties][logParties];
        for(int p=0; p<parties; p++) {
            for(int i=0; i<logParties; i++){
                answers[p][i] = getAtomicBoolean(name+"-"+p+"-"+i,false);
            }
        }
        AtomicCounter identity = getAtomicCounter(name+"-identity",-1);
        return new ScalableCyclicBarrier(name, parties, answers, identity);
    }

    public Semaphore getSemaphore(String name) {
        return factory.getInstanceOf(Semaphore.class, name);
    }

    public Semaphore getSemaphore(String name, int permits) {
        return factory.getInstanceOf(Semaphore.class, name, false, false, false, name, permits);
    }

    public AtomicLong getAtomicLong(String name) {
        return getAtomicLong(name, (long)0);
    }

    public AtomicLong getAtomicLong(String name, long initialValue) {
        return factory.getInstanceOf(AtomicLong.class, name, false, false, false, name, initialValue);
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
        return factory.getCache();
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
        AtomicCounter counter = getAtomicCounter(name, parties);
        return new CountDownLatch(name, parties, counter);
    }

    public void clear() {
        factory.clear();
    }

    public void close() {
        factory.close();
    }

}
