package org.crucial.dso;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Entity
@Command(name = "pipe")
public class Pipe {

    @Id
    @Option(names = "-n" )
    public String name = "pipe";
    public String ipport;

    private static final int BARRIER = 2;
    private AtomicCounter counter;

    public Pipe() {

    }

    public Pipe(String name) {

      this.name = name;      
    }
    
    public int wait()
    {
       int ret = counter.increment();

       while(ret < BARRIER) {
         try {
           Thread.currentThread().sleep(500);
         } catch (InterruptedException e) {
          // ignore
         }
       }

       return 0;
    }

    @Override
    @Command(name = "begin")
    public void begin(@Option(names = "-1") String ipport) {
      
      this.ipport = ipport;
      this.wait(); 
    
    }
   
    @Override
    @Command(name = "end")
    public void end(@Option(names = "-1") String ipport) {
      
      this.ipport = ipport;   
      this.wait();

    }

    @Override
    @Command(name = "printName")
    public String printName() {

      return this.name;
    }

}