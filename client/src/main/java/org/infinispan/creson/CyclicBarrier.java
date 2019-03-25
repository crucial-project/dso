package org.infinispan.creson;

public class CyclicBarrier {

    private AtomicCounter counter;
    private AtomicCounter generation;
    private int parties;

    public CyclicBarrier(){}

    public CyclicBarrier(String name, int parties){
        this.counter = new AtomicCounter(name,0);
        this.generation = new AtomicCounter(name,0);
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
        while (previous == current) {
            current = generation.tally();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return ret;
    }

}
