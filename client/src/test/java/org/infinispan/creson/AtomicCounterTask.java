package org.infinispan.creson;

import java.util.Random;

public class AtomicCounterTask extends Task {

    private Random random;

    public AtomicCounterTask(String[] parameters, int calls, int threads, int parallelism) {
        super(parameters, calls, threads, parallelism);
        random = new Random();
    }

    @Override
    public void doCall() {
        AtomicCounter counter = (AtomicCounter) instances.get(random.nextInt(instances.size()));
        counter.increment();
    }

    @Override
    public Object newObject(int id) {
        return new AtomicCounter("counter-test-"+id,0);
    }

    @Override
    public int checksum(){
        int checksum = 0;
        for (Object counter: instances) {
            checksum += ((AtomicCounter)counter).tally();
        }
        return checksum;
    }

}
