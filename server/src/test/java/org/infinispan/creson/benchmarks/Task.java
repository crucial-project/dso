package org.infinispan.creson.benchmarks;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class Task implements Callable<Double> {

    private int calls;
    protected List<Object> objects;
    protected String[] parameters;

    private List<Long> latencies, lastLatencies;
    private AtomicDouble throughput;
    private boolean isOver;

    public Task(List<Object> objects, String[] parameters, int calls) {
        this.calls = calls;
        this.objects = objects;
        this.parameters = parameters;

        latencies = new ArrayList<>();
        lastLatencies = Collections.synchronizedList(new ArrayList<>());
        throughput = new AtomicDouble(0);
        isOver = false;
    }

    public double getThroughput() {
        return throughput.get();
    }

    public double getLastThroughput() {
        double avrgLatency = 0;
        for (int i = 1; i <= lastLatencies.size(); i++) {
            avrgLatency += latencies.get(latencies.size() - i);
        }
        avrgLatency = avrgLatency / ((double) lastLatencies.size());
        avrgLatency = avrgLatency * Math.pow(10, -9);
        lastLatencies.clear();
        return ((double) 1) / avrgLatency;
    }

    public boolean getIsOver() {
        return isOver;
    }

    @Override
    public Double call() throws Exception {
        long beg = System.currentTimeMillis();
        // increment calls objects at random
        for (int t = 0; t < calls; t++) {
            long start = System.nanoTime();
            doCall();
            latencies.add(System.nanoTime() - start);
            lastLatencies.add(System.nanoTime() - start);
            double avrgLatency = 0; // average latency over the last 100 operations
            int count = Math.min(1000, latencies.size());
            for (int i = 1; i <= count; i++) {
                avrgLatency += latencies.get(latencies.size() - i);
            }
            avrgLatency = avrgLatency / ((double) count);
            avrgLatency = avrgLatency * Math.pow(10, -9);
            throughput.set(((double) 1) / avrgLatency);
        }
        isOver = true;
        return (Math.pow(10, -3) * ((double) (System.currentTimeMillis() - beg))) / (double) calls;
    }

    public abstract void doCall();

}
