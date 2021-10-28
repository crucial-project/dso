package org.crucial.dso;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Entity
@Command(name = "rdv")
public class RDV
{
    public String id;
    public String host;

    public void put(String h)
    {
        host = h;
    }

    public String get()
    {
        return host;
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