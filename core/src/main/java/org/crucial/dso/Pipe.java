package org.crucial.dso;


import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Entity
@Command(name = "pipe")
public class Pipe {

    @Id
    @Option(names = "-n" )
    public String name = "pipe";

    @Override
    @Command(name = "begin")
    public void begin() {
        
    }
   
    @Override
    @Command(name = "end")
    public void end(@Option(names = "-1") String ipport) {
        
    }
}