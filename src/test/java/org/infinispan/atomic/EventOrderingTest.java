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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.Assert.assertEquals;

/**
 * @author Pierre Sutra
 */

@Test(testName = "EventOrderingTest")
public class EventOrderingTest extends MultipleCacheManagersTest {

   protected static int NMANAGERS=8;
   protected static int NCALLS=1000;
   protected static int REPLICATION_FACTOR=3;
   protected static CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
   protected static boolean USE_TRANSACTIONS = false;

   @Test
   public void testEventOrdering() throws ExecutionException, InterruptedException {

      List<ClusterListener> listeners = new ArrayList<>();

      for(int i=0; i< NMANAGERS; i++) {
         Cache<Integer, Integer> cache = getCacheManagers().get(i).getCache();
         ClusterListener clusterListener= new ClusterListener();
         cache.addListener(clusterListener);
         listeners.add(clusterListener);
      }

      List<Future> futures = new ArrayList<>();
      for (EmbeddedCacheManager manager : getCacheManagers()) {
         futures.add(fork(new ExerciseEventTask(manager)));
      }

      for (Future future : futures) {
         future.get();
      }

      List<Object> list = null;
      for (ClusterListener listener : listeners) {
         if (list==null)
            list = listener.values;
         assertEquals(list, listener.values);
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
            manager.getCache().put(
                  1,
                  ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
         }
         return 0;
      }

   }

   @Listener(clustered = true, sync = true, includeCurrentState = true)
   public class ClusterListener{

      public List<Object> values= new ArrayList<>();

      @CacheEntryCreated
      @CacheEntryModified
      @CacheEntryRemoved
      public void onCacheEvent(CacheEntryEvent event) {
         int value = (int) event.getValue();
         if (!values.contains(value))
            values.add(event.getValue());
      }

   }


}
