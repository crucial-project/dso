package org.infinispan.creson.benchmarks.count;

import org.infinispan.creson.benchmarks.Task;
import org.infinispan.creson.concurrent.Counter;

import java.util.List;
import java.util.Random;

public class CounterTask extends Task {

    private Random random;
    private int size;

    public CounterTask(List<Object> objects, String[] parameters, int calls) {
        super(objects, parameters, calls);
        assert parameters == null;
        random = new Random();
    }

    @Override
    public void doCall() {
        Counter counter = (Counter) objects.get(random.nextInt(objects.size()));
        counter.increment();
    }
}
