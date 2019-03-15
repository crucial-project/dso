package org.infinispan.creson.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Task implements Callable<Double> {

    private int calls;
    protected List<Object> objects;
    protected String[] parameters;

    private List<Long> latencies;
    private Lock lock;
    private boolean isOver;

    public Task(List<Object> objects, String[] parameters, int calls) {
        this.calls = calls;
        this.objects = objects;
        this.parameters = parameters;

        latencies = new ArrayList<>();
        isOver = false;
        lock = new ReentrantLock();
    }

    public double getThroughput() {
        lock.lock();
        double avrgLatency = 0;
        for (int i = 1; i <= latencies.size(); i++) {
            avrgLatency += latencies.get(latencies.size() - i);
        }
        avrgLatency = avrgLatency / ((double) latencies.size());
        avrgLatency = avrgLatency * Math.pow(10, -9);
        latencies.clear();
        lock.unlock();
        return ((double) 1) / avrgLatency;
    }

    public boolean getIsOver() {
        lock.lock();
        boolean ret = isOver;
        lock.unlock();
        return ret;
    }

    @Override
    public Double call() throws Exception {
        long beg = System.currentTimeMillis();
        for (int t = 0; t < calls; t++) {
            long start = System.nanoTime();
            doCall();
            long stop = System.nanoTime() - start;
            lock.lock();
            latencies.add(stop);
            lock.unlock();
        }
        lock.lock();
        isOver = true;
        lock.unlock();
        return (Math.pow(10, -3) * ((double) (System.currentTimeMillis() - beg))) / (double) calls;
    }

    public abstract void doCall();

}
