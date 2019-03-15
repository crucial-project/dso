package org.infinispan.creson;

public class CyclicBarrier {

    private AtomicCounter counter;
    private int parties;

    public CyclicBarrier(){}

    public CyclicBarrier(String name, int parties){
        this.counter = new AtomicCounter(name,parties);
        this.parties = parties;
    }

    public int await(){
        int ret = counter.increment();
        long current = ret;
        while(current != 0) {
            current = counter.tally();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return ret;
    }

}
