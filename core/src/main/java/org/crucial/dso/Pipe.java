package org.crucial.dso;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "pipe")
public class Pipe {

    @Option(names = "-n" )
    public String name = "pipe";
    
    private AtomicCounter counter;
    private AtomicCounter generation;
    private AtomicReference<String> ipport;
    public int parties = 2;

    public Pipe() {}

    public Pipe(String name) {	
      System.out.println("Pipe constructor");
      this.name = name;
      this.counter = new AtomicCounter("counter-"+name);
      this.generation = new AtomicCounter("generation-"+name);
      this.ipport = new AtomicReference("ref-"+name);
    }
       
    public int waiting()
    {
      int previous = generation.tally();
      int ret = counter.increment();
      if (ret % parties == 0) {
          counter.reset();
          generation.increment();
      }

      System.out.println("waiting(): counter="+counter.tally()+", generation="+generation.tally()+", parties="+parties);

      int current = generation.tally();
      while (previous == current) {
          try {
              Thread.currentThread().sleep(500);
          } catch (InterruptedException e) {
              // ignore
          }
          current = generation.tally();
      }

      System.out.println("Pipe: barrier return");
        
      return ret;
    }
    

    @Command(name = "begin")
    public String begin() {
	    String ret = this.ipport.get();
      System.out.println("Pipe begin: call barrier, ret="+ret);
	    this.waiting(); 	
	    return ret;
    }
   
    @Command(name = "end")
    public void end(@Option(names = "-1") String ipport) {      
	    this.ipport.set(ipport);   
      System.out.println("Pipe end: call barrier, ipport="+ipport);
	    this.waiting();
    }

    @Command(name = "getName")
    public String getName() {
      return this.name;
    }

}
