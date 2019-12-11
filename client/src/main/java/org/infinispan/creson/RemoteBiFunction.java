package org.infinispan.creson;

import java.io.Serializable;
import java.util.function.BiFunction;

public interface RemoteBiFunction<U,R,V> extends BiFunction<U,R,V>, Serializable {}
