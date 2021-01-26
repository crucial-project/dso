package org.crucial.dso;


import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Entity
@Command(name = "pipe")
public class Pipe {

    @Id
    @Option(names = "-n" )
    public String name = "pipe";

    private static final int BARRIER = 2;
    private AtomicCounter counter;
    
    public void wait()
    {
       int ret = counter.increment();

       while(ret < BARRIER) {
         try {
           Thread.currentThread().sleep(500);
         } catch (InterruptedException e) {
          // ignore
         }
       }
    }

    @Override
    @Command(name = "begin")
    public void begin(@Option(names = "-1") String ipport) {

      this.wait(); 
    
    }
   
    @Override
    @Command(name = "end")
    public void end(@Option(names = "-1") String ipport) {
      
      this.wait();

    }
}