package org.crucial.dso;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.Map;
import java.util.function.BiFunction;

public interface RemoteBiFunction<U,R,V> extends BiFunction<U,R,V>, Serializable {
    Map<String, RemoteBiFunction<String,String,String>> BIFUNCTIONS = ImmutableMap.of(
            "sum", (x,y) -> Integer.toString(Integer.valueOf(x) + Integer.valueOf(y))
    );
}
