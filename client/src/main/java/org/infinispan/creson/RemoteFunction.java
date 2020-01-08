package org.infinispan.creson;

import java.io.Serializable;
import java.util.function.Function;

public interface RemoteFunction<T,R> extends Function<T,R>, Serializable {}
