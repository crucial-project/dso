package org.infinispan.creson;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AtomicCounter implements Comparable<AtomicCounter> {

    @Id
    public String name;
    public int count;

    public AtomicCounter(){}

    public AtomicCounter(String name, int value){
        this.name = name;
        this.count = value;
    }

    public int increment() {
        return increment(1);
    }

    public int increment(int inc){
        count+=inc;
        return count;
    }

    public int decrement() {
        return --count;
    }

    public int tally() {
        return count;
    }

    public int compareTo(AtomicCounter that) {
        if      (this.count < that.count) return -1;
        else if (this.count > that.count) return +1;
        else                              return  0;
    }

    public void reset() {
        count=0;
    }
}
