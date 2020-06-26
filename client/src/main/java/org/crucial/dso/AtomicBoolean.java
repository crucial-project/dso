package org.crucial.dso;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author Daniel
 */
@Entity
public class AtomicBoolean implements Serializable{

    @Id public String name;
    private boolean value;

    /**
     * Creates a new {@code AtomicBoolean} with the given initial value.
     *
     * @param initialValue the initial value
     */
    public AtomicBoolean(String name, boolean initialValue){
        this.name = name;
        this.value = initialValue;
    }

    /**
     * Creates a new {@code AtomicBoolean} with initial value {@code false}.
     */
    public AtomicBoolean(){
    }

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    public boolean get(){
        return value;
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public boolean compareAndSet(boolean expect, boolean update){
        if (! value ^ expect) {
            value = update;
            return true;
        }
        return false;
    }

    /**
     * Unconditionally sets to the given value.
     *
     * @param newValue the new value
     */
    public void set(boolean newValue){
        value = newValue;
    }

    /**
     * Atomically sets to the given value and returns the previous value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    public boolean getAndSet(boolean newValue){
        boolean prev;
        do {
            prev = get();
        } while (! compareAndSet(prev, newValue));
        return prev;
    }

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString(){
        return Boolean.toString(get());
    }
}
