package org.infinispan.creson.container.remote;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
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

public class RemoteContainer extends BaseContainer {

   private RemoteCache<Reference,Call> cache;

   public RemoteContainer(
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
      cache = (RemoteCache<Reference, Call>) c;
   }

   @Override
   public BasicCache getCache() {
      return cache;
   }

   @Override
   public void execute(Reference reference, Call call) {
      handleFuture((CallFuture) cache.withFlags(Flag.FORCE_RETURN_VALUE).put(reference, call));
   }
}
