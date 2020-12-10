package org.crucial.dso;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.persistence.Id;

@Entity
@Command(name = "set")
public class AtomicSet<V> implements Set<V>, Serializable {

    @Id
    @Option(names = "-n" )
    public String name = "set";

    public Set<V> delegate;

    public AtomicSet(){
        delegate = new HashSet<>();
    }

    public AtomicSet(String name){
        this.name = name;
        this.delegate = new HashSet<>();
    }

    @Override
    @Command(name = "size")
    public int size() {
        return delegate.size();
    }

    @Override
    @Command(name = "isEmpty")
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    @Command(name = "contains")
    public boolean contains(@Option(names = "-1") Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<V> iterator() {
        throw new IllegalStateException("invalid operation");
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return delegate.toArray(ts);
    }

    @Override
    public boolean add(V v) {
        return delegate.add(v);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return delegate.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends V> collection) {
        return delegate.addAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return delegate.retainAll(collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return delegate.removeAll(collection);
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}
