package org.infinispan.creson;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 *
 * @author Daniel
 */
@Entity
public class CFuture<V> implements java.util.concurrent.Future<V>, Externalizable{

    @Id
    public String name;
    public int status = 0; // 0 => init, 1 => done, -1 => cancelled
    public V v;

    public CFuture(){
    }

    public CFuture(String name){
        this.name = name;
    }

    public synchronized void set(V v){
        if (status != 0) return;
        this.v = v;
        status = 1;
        this.notifyAll();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning){
        throw new IllegalStateException();
    }

    @Override
    public synchronized V get() throws InterruptedException{
        if (status == 0)
            this.wait();
        return v;
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException{
        if (status == 0)
            this.wait(TimeUnit.MILLISECONDS.convert(timeout, unit));
        if (status == 0)
            throw new TimeoutException();
        return v;
    }

    @Override
    public boolean isCancelled(){
        return false;
    }

    @Override
    public synchronized boolean isDone(){
        return status == 1;
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException{
        objectOutput.writeInt(status);
        objectOutput.writeObject(v);
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException{
        status = objectInput.readInt();
        v = (V) objectInput.readObject();
    }
}
