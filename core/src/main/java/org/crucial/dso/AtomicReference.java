package org.crucial.dso;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;


@Entity
public class AtomicReference<T> implements Serializable {

    @Id public String name;
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
