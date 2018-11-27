package org.infinispan.crucial;

import java.io.Serializable;

/**
 *
 * @author Daniel
 */
public class CAtomicByteArray implements Serializable{
    private byte[] value;

    public CAtomicByteArray() {
    }

    public CAtomicByteArray(byte[] initialValue) {
        value = initialValue;
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
