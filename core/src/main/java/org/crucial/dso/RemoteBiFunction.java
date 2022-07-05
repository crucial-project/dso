package org.crucial.dso;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;

public interface RemoteBiFunction<U, R, V> extends BiFunction<U, R, V>, Serializable {
    Map<String, RemoteBiFunction<String, String, String>> BIFUNCTIONS = Collections.singletonMap(
            "sum", (x, y) -> Integer.toString(Integer.parseInt(x) + Integer.parseInt(y))
    );
}
