package org.infinispan.atomic.container.remote;

import org.infinispan.atomic.container.BaseContainer;
import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.atomic.object.Call;
import org.infinispan.atomic.object.CallFuture;
import org.infinispan.atomic.object.Reference;
import org.infinispan.atomic.utils.UUIDGenerator;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.commons.api.BasicCache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Pierre Sutra
 */

public class RemoteContainer extends BaseContainer {

   private static Map<BasicCache,Listener> listeners = new ConcurrentHashMap<>();
   private static synchronized UUID installListener(BasicCache cache) {
      if (!listeners.containsKey(cache)) {
         Listener listener = new Listener();
         ((RemoteCacheImpl) cache).addClientListener(listener, new Object[] { listener.getId() }, null);
         listeners.put(cache, listener);
         if (log.isTraceEnabled()) log.trace("Remote listener "+listener.getId()+" installed");
      }
      return listeners.get(cache).getId();
   }

   private static synchronized void removeListener(BasicCache cache) {
      if (listeners.containsKey(cache)) {
         Listener listener = listeners.get(cache);
         ((RemoteCacheImpl) cache).removeClientListener(listener);
         listeners.remove(cache);
      }
   }

   private UUID listenerID;
   private RemoteCache<Reference,Call> cache;
   
   public RemoteContainer(
         BasicCache c,
         Reference reference,
         boolean readOptimization,
         boolean forceNew,
         Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException,
         ExecutionException, NoSuchMethodException, InvocationTargetException, TimeoutException {
      super(reference, readOptimization, forceNew, initArgs);
      cache = (RemoteCache<Reference, Call>) c;
      listenerID = installListener(cache);
   }


   @Override
   public synchronized void open()
         throws InterruptedException, ExecutionException, TimeoutException {
      installListener(cache);
      super.open();
   }

   @Override
   public synchronized void close()
         throws InterruptedException, ExecutionException, TimeoutException {
      super.close();
      removeListener(cache);
   }

   @Override
   public UUID listenerID() {
      return listenerID;
   }

   @Override
   public void put(Reference reference, Call call) {
      cache.put(reference,call);
   }

   @Override public BasicCache getCache() {
      return null;  // TODO: Customise this generated block
   }

   @ClientListener(
         filterFactoryName = FilterConverterFactory.FACTORY_NAME,
         converterFactoryName= FilterConverterFactory.FACTORY_NAME)
   private static class Listener{

      private UUID id;

      public Listener(){
         id = UUIDGenerator.generate();
      }

      public UUID getId(){
         return id;
      }

      @Deprecated
      @ClientCacheEntryModified
      @ClientCacheEntryCreated
      public void onCacheModification(ClientCacheEntryCustomEvent event){
         CallFuture future = (CallFuture) event.getEventData();
         handleFuture(future);
      }
      
      
   }

}
