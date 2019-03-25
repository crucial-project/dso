package org.infinispan.creson;

import java.util.Random;

public class AtomicCounterTask extends Task {

    private Random random;

    public AtomicCounterTask(String[] parameters, int calls, int clients) {
        super(parameters, calls, clients);
        assert parameters == null;
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

}
