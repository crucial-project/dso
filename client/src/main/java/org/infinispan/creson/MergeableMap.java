package org.infinispan.creson;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * ...
 * <p>
 * Date: 2018-02-07
 *
 * @author Daniel
 */
public interface MergeableMap<K, V> extends Map<K, V>{

    default void mergeAll(Map<? extends K, ? extends V> m, BiFunction<? super V, ? super V, ? extends V> f){
        m.forEach((k, v) -> merge(k, v, f));
    }
}
