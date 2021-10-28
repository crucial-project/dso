package org.crucial.dso;

import java.util.concurrent.TimeUnit;

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
        while (this.host == "")
        {
            TimeUnit.SECONDS.sleep(1);
        }

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