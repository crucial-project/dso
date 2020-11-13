package org.crucial.dso;

public class CountDownLatch{

    private AtomicCounter counter;
    private int parties;

    public CountDownLatch(String name, int parties, AtomicCounter counter){
        this.parties = parties;
        this.counter = counter;
    }

    public int await(){
        int ret = countDown();
        long current = getCount();
        while(current != 0) {
            current=getCount();
        }
        return ret;
    }

    public int countDown(){
        return this.counter.decrement();
    }

    public int getCount(){
        return this.counter.tally();
    }

}
