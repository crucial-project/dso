package org.infinispan.creson;

public class CountDownLatch {

    private AtomicCounter counter;
    private int parties;

    public CountDownLatch(String name, int parties){
        this.parties = parties;
        this.counter = new AtomicCounter(name, parties);
    }

    public int await(){
        int ret = countDown();
        long current = getCount();
        while(current != 0) {
            current=getCount();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return ret;
    }

    public int countDown(){
        return this.counter.decrement();
    }

    public int getCount(){
        return counter.tally();
    }

    public void reset(){
        counter.increment(this.parties);
    }

}
