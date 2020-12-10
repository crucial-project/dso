package org.crucial.dso;

import picocli.CommandLine;

import javax.persistence.Id;
import java.io.Serializable;

/**
 *
 * @author Daniel
 */
@CommandLine.Command(name = "bytearray")
public class AtomicByteArray implements Serializable{

    @Id
    @CommandLine.Option(names = "-n" )
    public String name = "cnt";

    private byte[] value;

    public AtomicByteArray() {}

    public AtomicByteArray(String name, byte[] value) {
        this.name = name;
        this.value = value;
    }

    public void printValue() {
        System.out.println(toString());
    }

    public byte[] get() {
        return value;
    }

    public void set(byte[] newValue) {
        value = newValue;
    }

    public byte[] getAndSet(byte[] newValue) {
        byte[] old = value;
        value = newValue;
        return old;
    }

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString() {
        return new String(get());
    }
}
