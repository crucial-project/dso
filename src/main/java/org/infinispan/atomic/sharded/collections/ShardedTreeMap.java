package org.infinispan.atomic.sharded.collections;

import org.infinispan.atomic.Distributed;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

/**
 *
 * A sorted map abstraction implemented via an ordered forest of trees.
 * The ordered forest of trees is stored in variable <i>forest</i>.
 * Each trees is a shared object implemented with the atomic object factory.
 * It contains at most <i>threshold</i> objects.
 *
 * @author Pierre Sutra
 * @since 7.2
 *
 */
@Distributed
public class ShardedTreeMap<K extends Comparable<K>,V> implements SortedMap<K, V>, Externalizable
{

    private static Log log = LogFactory.getLog(ShardedTreeMap.class);
    private final static int DEFAULT_THRESHOLD = 1000;

    private SortedMap<K,TreeMap<K,V>> forest; // the ordered forest
    private int threshold; // how many entries are stored before creating a new tree in the forest.

    public ShardedTreeMap(){
        forest = new TreeMap<>();
        threshold = DEFAULT_THRESHOLD;
    }

    public ShardedTreeMap(Integer threshhold){
        assert threshhold>=1;
        forest = new TreeMap<>();
        this.threshold = threshhold;
    }

    @Override
    public SortedMap<K, V> subMap(K k, K k2) {
        SortedMap<K,V> result = new TreeMap<>();
        for(K key : forest.keySet()){
            if (key.compareTo(k2) > 0)
                break;
            result.putAll(forest.get(key).subMap(k, k2));
        }
        return result;
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        SortedMap<K,V> result = new TreeMap<>();
        for(K key : forest.keySet()){
            if (key.compareTo(toKey) > 0)
                break;
            result.putAll(forest.get(key).headMap(toKey));
        }
        return result;
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        SortedMap<K,V> result = new TreeMap<>();
        for(K key : forest.keySet()){
            result.putAll(forest.get(key).tailMap(fromKey));
        }
        return result;
    }

    @Override
    public K firstKey() {
        if(forest.isEmpty())
            return null;
        assert !forest.get(forest.firstKey()).isEmpty() : forest.toString();
        K ret = forest.get(forest.firstKey()).firstKey();
        return ret;
    }

    @Override
    public K lastKey() {
        if(forest.isEmpty())
            return null;
        K last = forest.lastKey();
        if (!forest.get(last).isEmpty()) {
            last = forest.get(last).lastKey();
        }
        return last;
    }

    @Override
    public int size() {
        int ret = 0;
        for(K v: forest.keySet()){
            ret+= forest.get(v).size();
        }
        return ret;
    }

    @Override
    public boolean isEmpty() {
        return forest.isEmpty();
    }

    @Override
    public V get(Object o) {
        if (forest.isEmpty())
            return null;
        K last = forest.lastKey();
        TreeMap<K,V> treeMap = forest.get(last);
        assert !treeMap.isEmpty();
        V ret = treeMap.lastEntry().getValue();
        return ret;
    }

    @Override
    public V put(K k, V v) {
        V ret = doPut(k,v);
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return forest.hashCode();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (forest.isEmpty()){
            TreeMap<K,V> treeMap = new TreeMap<>(map);
            int split = treeMap.size()/this.threshold+1;
            K beg = treeMap.firstKey();
            for(int i=0; i<split; i++){
                TreeMap<K,V> sub = forest.get(beg);
                forest.put(beg,sub);
                TreeMap<K,V> toAdd = new TreeMap<>();
                for(K k : treeMap.tailMap(beg).keySet()){
                    if(toAdd.size()==threshold){
                        beg = k;
                        break;
                    }
                    toAdd.put(k,treeMap.get(k));
                }
                sub.putAll(toAdd);
            }
        }else{
            for(K k : map.keySet()){
                doPut(k, map.get(k));
            }
        }
    }

    @Override
    public String toString(){
        TreeMap<K,V> all = new TreeMap<K, V>();
        for(K key : forest.keySet()){
           all.putAll(forest.get(key));
        }
        return all.toString();
    }

    //
    // MARSHALLING
    //

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeObject(new ArrayList<>(forest.keySet()));
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        forest = new TreeMap<>();
        for( K k : (List<K>)objectInput.readObject()){
            forest.put(k,null);
        }
    }


    //
    // HELPERS
    //

    private V doPut(K k, V v){
        log.debug("adding " + k + "=" + v);

        V ret;
        K key;
        TreeMap<K,V> tree;
        SortedMap<K,TreeMap<K,V>> headMap;

        // 1 - Find the tree where to retrieve (k,v)
        headMap = forest.headMap(k);
        if (!headMap.isEmpty() && headMap.get(headMap.lastKey()).size() < threshold)
            key = headMap.lastKey();
        else
            key = k;

        // 2 - retrieve (k,v)
        tree = forest.get(key);
        ret = tree.put(k,v);

        log.debug("in tree "+key+" -> "+tree);

        // 3 - update the forest if needed
        // 3.1 - change the GLB element in the tree
        if (key!=k && tree.firstKey().equals(k)) {
            forest.remove(key);
            forest.put(k,tree);
        }

        // 3.2 -split the tree if needed
        if (tree.size() > threshold) {
            Entry<K,V> entry = tree.lastEntry();
            tree.remove(entry.getKey());
            put(entry.getKey(),entry.getValue());
        }

        return ret;

    }

    @Override
    public boolean containsKey(Object o) {
        for(K k : forest.keySet()){            
            if(forest.get(k).containsKey(o))
                return true;
        }
        return false;
    }

    //
    // NOT YET IMPLEMENTED
    //

    @Override
    public V remove(Object o) {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public boolean containsValue(Object o) {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Comparator<? super K> comparator() {
        throw new UnsupportedOperationException("to be implemented");
    }

}
