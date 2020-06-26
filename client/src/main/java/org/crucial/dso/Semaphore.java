package org.crucial.dso;

/**
 * @author Daniel
 */
public class Semaphore {
    private int permits;
    public Semaphore(){}

    public Semaphore(int permits){
        this.permits = permits;
    }

    public synchronized void acquire() throws InterruptedException{
        while (permits<=0) this.wait();
        permits--;
    }
    public synchronized void release(){
        permits++;
        this.notify();
    }
}
