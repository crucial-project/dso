package org.infinispan.creson.container.local;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.creson.container.BaseContainer;
import org.infinispan.creson.object.Call;
import org.infinispan.creson.object.CallFuture;
import org.infinispan.creson.object.Reference;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Pierre Sutra
 */
public class LocalContainer extends BaseContainer {

    private AdvancedCache<Reference, Call> cache;

    public LocalContainer(
            BasicCache c,
            Class clazz,
            Object key,
            boolean readOptimization,
            boolean forceNew,
            Object... initArgs)
            throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
            InterruptedException,
            ExecutionException, NoSuchMethodException, InvocationTargetException, TimeoutException, NoSuchFieldException {
        super(clazz, key, readOptimization, forceNew, initArgs);
        this.cache = ((org.infinispan.Cache) c).getAdvancedCache();
        if (log.isTraceEnabled()) log.trace(this + "Created successfully");
    }

    @Override
    public BasicCache getCache() {
        return cache;
    }

    @Override
    public void execute(Reference reference, Call call) {
        handleFuture((CallFuture) getCache().put(reference, call));
    }
}
