package org.crucial.dso;

import org.crucial.dso.client.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Task implements Callable<Double> {

    private long taskId;
    private int calls;
    private List<Long> latencies;
    private Lock lock;
    private boolean isOver;

    protected Client client;
    protected String[] parameters;
    protected int threads;
    protected int parallelism;
    protected List<Object> instances;

    public Task(long taskId, String[] parameters, int calls, int threads, int parallelism, Client client) {
        this.taskId = taskId;
        this.threads = threads;
        this.calls = calls;
        this.parameters = parameters;
        this.parallelism = parallelism;

        this.latencies = new ArrayList<>();
        this.isOver = false;
        this.lock = new ReentrantLock();

        this.client = client;
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

    public void setInstances(List<Object> instances){
        this.instances = instances;
    }

    public abstract void doCall();

    public abstract Object newObject(int id);

    public int checksum(){
        return 0;
    }

}
