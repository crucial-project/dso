package org.infinispan.creson;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.*;

@Entity
@Command(name = "list")
public class AtomicList<E> implements List<E> {

    @Id
    @Option(names = "-n" )
    public String name = "list";

    private List<E> delegate;

    public AtomicList(){}

    public AtomicList(String name){
        this.name = name;
        this.delegate = new ArrayList<>();
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
    public Iterator<E> iterator() {
        throw new IllegalStateException();
    }

    @Override
    @Command(name = "toArray")
    public Object[] toArray() {
        return (Object[]) delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        throw new IllegalStateException();
    }

    @Override
    @Command(name = "add")
    public boolean add(@Option(names = "-1") E e) {
        return delegate.add(e);
    }

    @Override
    @Command(name = "remove")
    public boolean remove(@Option(names = "-1") Object o) {
        return delegate.remove(o);
    }

    @Override
    @Command(name = "containsAll")
    public boolean containsAll(@Option(names = "-1") Collection<?> collection) {
        return delegate.containsAll(collection);
    }

    @Override
    @Command(name = "addAll")
    public boolean addAll(@Option(names = "-1") Collection<? extends E> collection) {
        return delegate.addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> collection) {
        throw new IllegalStateException();
    }

    @Override
    @Command(name = "removeAll")
    public boolean removeAll(@Option(names = "-1") Collection<?> collection) {
        return delegate.removeAll(collection);
    }

    @Override
    @Command(name = "retainAll")
    public boolean retainAll(@Option(names = "-1") Collection<?> collection) {
        return delegate.retainAll(collection);
    }

    @Override
    @Command(name = "clear")
    public void clear() {
        delegate.clear();
    }

    @Override
    @Command(name = "get")
    public E get(@Option(names = "-1") int i) {
        return delegate.get(i);
    }

    @Override
    @Command(name = "set")
    public E set(@Option(names = "-1") int i, @Option(names = "-2") E e) {
        return delegate.set(i,e);
    }

    @Override
    public void add(int i, E e) {
        throw new IllegalStateException();
    }

    @Override
    public E remove(int i) {
        throw new IllegalStateException();
    }

    @Override
    @Command(name = "indexOf")
    public int indexOf(@Option(names = "-1") Object o) {
        return delegate.indexOf(o);
    }

    @Override
    @Command(name = "lastIndexOf")
    public int lastIndexOf(@Option(names = "-1") Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        throw new IllegalStateException();
    }

    @Override
    public ListIterator<E> listIterator(int i) {
        throw new IllegalStateException();
    }

    @Override
    @Command(name = "subList")
    public List<E> subList(@Option(names = "-1") int i, @Option(names = "-2") int j) {
        return new ArrayList<>(delegate.subList(i,j)); // inner class
    }
}
