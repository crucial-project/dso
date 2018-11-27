package org.infinispan.crucial;


/**
 * @author Daniel
 */
public class CSemaphore{
    private int permits;
    public CSemaphore(){}

    public CSemaphore(int permits){
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
