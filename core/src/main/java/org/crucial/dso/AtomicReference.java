package org.crucial.dso;

import java.io.Serializable;

public class AtomicReference<T> implements Serializable {

    public String name;
    private T value;

    public AtomicReference(){}

    public AtomicReference(String name, T v){
        this.name = name;
        this.value = v;
    }

    public T get(){
        return value;
    }

    public void set(T v){
        this.value = v;
    }
}
