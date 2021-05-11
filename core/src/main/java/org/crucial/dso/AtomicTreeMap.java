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
import java.util.function.Function;
import java.util.stream.Collectors;


@Entity
@Command(name = "treemap")
public class AtomicTreeMap<K,V> implements MergeableMap<K,V>, Serializable, SortedMap<K,V> {

    @Id
    @Option(names = "-n" )
    public String name = "map";

    public TreeMap<K,V> delegate;

    public AtomicTreeMap(){
        this.delegate = new TreeMap<>();
    }

    public AtomicTreeMap(String name){
        this.name = name;
        this.delegate = new TreeMap<>();
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
    public Comparator<? super K> comparator() {
        throw new IllegalStateException();
    }

    @Override
    @Command(name="subMap")
    public SortedMap<K, V> subMap(@Option(names = "-1") K k, @Option(names = "-2") K k1) {
        return delegate.subMap(k,k1);
    }

    @Override
    @Command(name="headMap")
    public SortedMap<K, V> headMap(@Option(names = "-1") K k) {
        return delegate.headMap(k);
    }

    @Override
    @Command(name="tailMap")
    public SortedMap<K, V> tailMap(@Option(names = "-1") K k) {
        return delegate.tailMap(k);
    }

    @Override
    @Command(name="firstKey")
    public K firstKey() {
        return delegate.firstKey();
    }

    @Override
    @Command(name="lastKey")
    public K lastKey() {
        return delegate.lastKey();
    }

    @Override
    @Command(name = "keySet")
    public Set<K> keySet() {
        Set<K> ret = new TreeSet<>(delegate.comparator());
        ret.addAll(delegate.keySet()); // inner class
        return ret;
    }

    @Override
    @Command(name = "values")
    public Collection<V> values() {
        return new ArrayList<>(delegate.values()); // inner class
    }

    @Command(name = "toString")
    public String toString() {
        return delegate.toString();
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

    @Command(name = "delegate")
    public Map<K,V> delegate(){
        return this.delegate;
    }

    // FIXME
    @Command(name = "reverse")
    public void reverse(@Option(names = "-1") boolean forceIntValues){
        SortedMap newmap = new TreeMap<>();
        for (Entry e : delegate.entrySet()){
            newmap.put(forceIntValues ? Integer.valueOf(e.getValue().toString()) : e.getValue(), e.getKey());
        }
        this.delegate = (TreeMap<K, V>) newmap;
    }

    @Command(name = "top")
    public Map<K,V> top(@Option(names = "-1") int k){
        SortedMap<K,V> tmp = new TreeMap<>(delegate.comparator());
        tmp.putAll(delegate.descendingMap().keySet().stream().limit(k).collect(Collectors.toMap(Function.identity(), delegate::get)));
        Map<K,V> reverseSortedMap = new TreeMap<K,V>(Collections.reverseOrder());
        reverseSortedMap.putAll(tmp);

        return reverseSortedMap;
    }

}
