package org.infinispan.crucial;


import java.io.Serializable;

/**
 *
 * @author Daniel
 */
public class CAtomicInt implements Serializable{
    private int value = 0;

    public CAtomicInt(){
    }

    public CAtomicInt(int initialValue){
        value = initialValue;
    }

    public void printValue(){
        System.out.println(value);
    }

    public int get(){
        return value;
    }

    public void set(int newValue){
        value = newValue;
    }

    public int getAndSet(int newValue){
        int old = value;
        value = newValue;
        return old;
    }

    public int getAndIncrement(){
        return getAndAdd(1);
    }

    public int getAndDecrement(){
        return getAndAdd(- 1);
    }

    public int getAndAdd(int delta){
        value += delta;
        return value - delta;
    }

    public int incrementAndGet(){
        return addAndGet(1);
    }

    public int decrementAndGet(){
        return addAndGet(- 1);
    }

    public int addAndGet(int delta){
        value += delta;
        return value;
    }


    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString(){
        return Integer.toString(get());
    }

    /**
     * Returns the value of this {@code CAtomicInt} as an {@code int}.
     */
    public int intValue(){
        return get();
    }

    /**
     * Returns the value of this {@code CAtomicInt} as a {@code long}
     * after a widening primitive conversion.
     */
    public long longValue(){
        return (long) get();
    }

    /**
     * Returns the value of this {@code CAtomicInt} as a {@code float}
     * after a widening primitive conversion.
     */
    public float floatValue(){
        return (float) get();
    }

    /**
     * Returns the value of this {@code CAtomicInt} as a {@code double}
     * after a widening primitive conversion.
     */
    public double doubleValue(){
        return (double) get();
    }
}
