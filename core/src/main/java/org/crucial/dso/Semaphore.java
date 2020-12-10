package org.crucial.dso;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author Daniel
 */

@Entity
public class Semaphore implements Serializable {

    private int permits;

    @Id
    private String name;

    public Semaphore(){}

    public Semaphore(String name, int permits){
        this.name = name;
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
