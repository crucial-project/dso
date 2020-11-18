package org.crucial.dso;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Entity
@Command(name = "map")
public class AtomicMap<K,V> implements MergeableMap<K,V>, Serializable {

    @Id
    @Option(names = "-n" )
    public String name = "map";

    public Map<K,V> delegate;

    public AtomicMap(){
        this.delegate = new HashMap<>();
    }

    public AtomicMap(String name){
        this.name = name;
        this.delegate = new HashMap<>();
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
    @Command(name = "containsKey")
    public boolean containsKey(@Option(names = "-1") Object o) {
        return delegate.containsKey(o);
    }

    @Override
    @Command(name = "containsValue")
    public boolean containsValue(@Option(names = "-1") Object o) {
        return delegate.containsValue(o);
    }

    @Command(name = "get")
    public V get(@Option(names = "-1") Object o) {
        return delegate.get(o);
    }

    @Override
    @Command(name = "put")
    public V put(@Option(names = "-1") K k, @Option(names = "-2") V v) {
        return delegate.put(k,v);
    }

    @Override
    @Command(name = "remove")
    public V remove(@Option(names = "-1") Object o) {
        return delegate.remove(o);
    }

    @Override
    @Command(name = "putAll")
    public void putAll(@Option(names = "-1") Map<? extends K, ? extends V> map) {
        delegate.putAll(map);
    }

    @Override
    @Command(name = "clear")
    public void clear() {
        delegate.clear();
    }

    @Override
    @Command(name= "foreach")
    public void forEach(@Option(names = "-1") BiConsumer<? super K, ? super V> c) { delegate.forEach(c);}

    @Override
    @Command(name = "keySet")
    public Set<K> keySet() {
        return new HashSet<>(delegate.keySet()); // inner class
    }

    @Override
    @Command(name = "values")
    public Collection<V> values() {
        return new ArrayList<>(delegate.values()); // inner class
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new IllegalStateException();
    }

    @Override
    @Command(name= "merge")
    public V merge(@Option(names = "-1") K k, @Option(names = "-2") V v, @Option(names = "-3") BiFunction<? super V, ? super V, ? extends V> f) {
        return delegate.merge(k, v, f);
    }

    @Command(name= "mergeAll")
    public void mergeAl(@Option(names = "-1") Map<? extends K, ? extends V> m, @Option(names = "-2") BiFunction<? super V, ? super V, ? extends V> f){
        MergeableMap.super.mergeAll(m, f);
    }

}
