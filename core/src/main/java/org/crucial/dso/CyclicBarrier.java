package org.crucial.dso;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.Serializable;

@Command(name = "barrier")
public class CyclicBarrier {

    private static final int MAGIC = 10;

    @Option(names = "-n" )
    public String name = "barrier";

    @Option(names = "-p" )
    public int parties = 1;
    private AtomicCounter counter;
    private AtomicCounter generation;

    @Deprecated
    public CyclicBarrier(){}

    public CyclicBarrier(String name, int parties, AtomicCounter counter, AtomicCounter generation){
        this.name = name;
        this.counter = counter;
        this.generation = generation;
        this.parties = parties;
    }

    @Command(name = "reset")
    public void reset(){
        counter.reset();
        generation.reset();
    }

    @Command(name = "await")
    public int await(){
        int previous = generation.tally();
        int ret = counter.increment();
        if (ret % parties == 0) {
            counter.reset();
            generation.increment();
        }

        int current = generation.tally();
        int backoff = (parties - Math.abs(ret % parties))/MAGIC;
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
