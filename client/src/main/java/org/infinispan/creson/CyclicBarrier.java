package org.infinispan.creson;

public class CyclicBarrier {

    private static final int MAGIC = 10;

    private AtomicCounter counter;
    private AtomicCounter generation;
    private int parties;

    public CyclicBarrier(){}

    public CyclicBarrier(String name, int parties){
        this.counter = new AtomicCounter(name+"-counter",0);
        this.generation = new AtomicCounter(name+"generation",0);
        this.parties = parties;
    }

    public int await(){
        int previous = generation.tally();
        int ret = counter.increment();
        if (ret % parties == 0) {
            counter.reset();
            generation.increment();
        }

        int current = generation.tally();
        int backoff = (parties - ret)/MAGIC;
        while (previous == current) {
            try {
                Thread.currentThread().sleep(backoff);
            } catch (InterruptedException e) {
                // ignore
            }
            current = generation.tally();
        }
        return ret;
    }

}
