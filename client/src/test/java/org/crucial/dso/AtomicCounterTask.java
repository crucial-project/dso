package org.crucial.dso;

import java.util.Random;

public class AtomicCounterTask extends Task {

    private Random random;

    public AtomicCounterTask(long taskId, String[] parameters, int calls, int threads, int parallelism) {
        super(taskId, parameters, calls, threads, parallelism);
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
