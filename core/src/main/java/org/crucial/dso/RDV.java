package org.crucial.dso;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Entity
@Command(name = "rdv")
public class RDV
{
    @Id
    @Option(names = "-n" )
    public String name = "rdv";
    public String host;

    @Override
    @Command(name = "put")
    public void put(@Option(names = "-1") String h)
    {
        this.host = h;
    }

    @Command(name = "get")
    public String get()
    {
        return this.host;
    }

    public void clear()
    {
        host = "";
    }

    public String value()
    {
        return host;
    }


}