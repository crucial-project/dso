package org.crucial.dso;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "barrier")
public class CyclicBarrier {

    private static final int MAGIC = 10;

    @Option(names = "-n" )
    public String name = "barrier";

    @Option(names = "-p" )
    public int parties = 1;
    private AtomicCounter counter;
    private AtomicCounter generation;

    public CyclicBarrier(){}

    public CyclicBarrier(String name, int parties){
        this.name = name;
        this.counter = new AtomicCounter(name+"-counter",0);
        this.generation = new AtomicCounter(name+"-generation",0);
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
        // System.out.println(ret+" - ("+current+","+previous+")");
        while (previous == current) {
            try {
                Thread.currentThread().sleep(backoff);
            } catch (InterruptedException e) {
                // ignore
            }
            // System.out.println(ret+" - ("+current+","+previous+")");
            current = generation.tally();
        }
        return ret;
    }

}
