package org.crucial.dso;

import java.util.concurrent.TimeUnit;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Entity
@Command(name = "rdv")
public class RDV
{
    public String name = "rdv";
    public String host;

    public RDV(String name)
    {
        this.name = name;
    }

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
            TimeUnit.MILLISECONDS.sleep(500);
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