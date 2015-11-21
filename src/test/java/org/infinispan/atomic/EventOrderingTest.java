package org.infinispan.atomic;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.test.AbstractCacheTest;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Pierre Sutra
 */

@Test(testName = "EventOrderingTest")
public class EventOrderingTest extends MultipleCacheManagersTest {

   protected static int NMANAGERS=5;
   protected static int NCALLS=1000;
   protected static int REPLICATION_FACTOR=3;
   protected static CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
   protected static boolean USE_TRANSACTIONS = false;
   protected static ConcurrentMap<Integer,Integer> added = new ConcurrentHashMap<>();

   @Test
   public void testEventOrdering() throws ExecutionException, InterruptedException {

      List<ClusterListener> listeners = new ArrayList<>();

      for(int i=0; i< NMANAGERS; i++) {
         Cache<Integer, Integer> cache = getCacheManagers().get(i).getCache();
         ClusterListener clusterListener= new ClusterListener();
         cache.addListener(clusterListener);
         listeners.add(clusterListener);
         if (listeners.size()==2) break;
      }

      List<Future> futures = new ArrayList<>();
      for (EmbeddedCacheManager manager : getCacheManagers()) {
         futures.add(fork(new ExerciseEventTask(manager)));
      }

      for (Future future : futures) {
         future.get();
      }

      ConcurrentMap<Integer,Integer> map= null;
      for (ClusterListener listener : listeners) {
         if (map==null)
            map= listener.received;
         assertTrue(listener.received.keySet().containsAll(added.keySet()));
         assertEquals(map, listener.received);
      }

   }

   //
   // Helpers
   //

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder builder
            = AbstractCacheTest.getDefaultClusteredCacheConfig(CACHE_MODE, USE_TRANSACTIONS);
      builder.clustering().hash().numOwners(REPLICATION_FACTOR);
      TransportFlags flags = new TransportFlags();
      createClusteredCaches(NMANAGERS, builder, flags);
   }

   public class ExerciseEventTask implements Callable<Integer> {

      private EmbeddedCacheManager manager;

      public ExerciseEventTask(EmbeddedCacheManager m) {
         manager = m;
      }

      @Override
      public Integer call() throws Exception {
         for (int i = 0; i < NCALLS; i++) {
            int value = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            while(added.putIfAbsent(value, value)!=null){
               value = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            }
            manager.getCache().put(1,value);
         }
         return 0;
      }

   }

   @Listener(clustered = true, sync = true, includeCurrentState = true)
   public class ClusterListener{

      public ConcurrentMap<Integer,Integer> received = new ConcurrentHashMap<>();

      @CacheEntryCreated
      @CacheEntryModified
      @CacheEntryRemoved
      public void onCacheEvent(CacheEntryEvent event) {
         int value = (int) event.getValue();
         received.putIfAbsent(value,value);
      }

   }


}
