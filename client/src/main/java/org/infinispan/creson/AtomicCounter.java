package org.infinispan.creson;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Command(name = "counter")
public class AtomicCounter implements Comparable<AtomicCounter> {

    @Id
    @Option(names = "-n" )
    public String name = "cnt";

    @Option(names = "-c" )
    public int count = 0;

    public AtomicCounter(){}

    public AtomicCounter(String name) {
        this.name = name;
    }

    public AtomicCounter(String name, int value){
        this.name = name;
        this.count = value;
    }

    public int increment() {
        return increment(1);
    }

    @Command(name = "increment")
    public int increment(@Option(names = "-i") int inc){
        count+=inc;
        return count;
    }

    public int decrement() {
        return --count;
    }

    @Command(name = "tally")
    public int tally() {
        return count;
    }

    @Override
    public int compareTo(AtomicCounter that) {
        if      (this.count < that.count) return -1;
        else if (this.count > that.count) return +1;
        else                              return  0;
    }

    @Command(name = "reset")
    public void reset() {
        count=0;
    }

}
