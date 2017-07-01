package org.infinispan.creson;

import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.infinispan.creson.CresonModuleLifeCycle.CRESON_CACHE_NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Pierre Sutra
 */

@Test(testName = "ISPNTest")
public class ISPNTest extends BaseTest {

   protected static ConcurrentMap<Integer,Integer> added = new ConcurrentHashMap<>();

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

      ConcurrentMap<Integer,Integer> map= null;
      for (ClusterListener listener : listeners) {
         if (map==null)
            map= listener.received;
         assertTrue(listener.received.keySet().containsAll(added.keySet()));
         assertEquals(map, listener.received);
      }

   }

   @Test
   public void testElasticity() throws ExecutionException, InterruptedException {

      List<Future> futures = new ArrayList<>();
      List<ExercisePutTask> tasks = new ArrayList<>();
      Map<Integer,Integer> map = new ConcurrentHashMap<>();

      for(int i=0; i<NMANAGERS; i++){
         ExercisePutTask putTask = new ExercisePutTask(
                 map,container(i).getCache(CRESON_CACHE_NAME));
         futures.add(fork(putTask));
         tasks.add(putTask);
      }

      try {
         // FIXME for some reason, we cannot do it outside of this thread
         ExerciseElasticityTask elasticityTask = new ExerciseElasticityTask();
         elasticityTask.call();
      } catch (Exception e) {
         e.printStackTrace();
      }

      for (HaltableTask task : tasks) {
         task.halt();
      }

      for (Future future : futures) {
         future.get();
      }

      for (int k : map.keySet()) {
         assert container(0).getCache(CRESON_CACHE_NAME).containsKey(k);
      }

   }

   //
   // Helpers
   //

   private class ExerciseElasticityTask implements  Callable<Integer> {

      @Override
      public Integer call() throws Exception {
         for (int i=0; i<10; i++) {
            Thread.sleep(3000);
            if (containers().size() == NMANAGERS) {
               addContainer();
            } else {
               deleteContainer();
            }
         }
         System.out.println(this.getClass() + " over.");
         return 0;
      }
   }

   public interface  HaltableTask extends Callable<Integer> {
      void halt();
   }

   private class ExerciseGetTask implements HaltableTask{

      private AtomicBoolean halted = new AtomicBoolean(false);
      private List<Integer> list;

      public ExerciseGetTask(List<Integer> list){
         this.list = list;
      }

      @Override
      public void halt() {
         halted.set(true);
      }

      @Override
      public Integer call() throws Exception {
         Random rand  = new Random(System.currentTimeMillis());
         while(!halted.get()) {
            int k = list.get(rand.nextInt(list.size()));
            assert container(0).getCache().get(k) != null;
            Thread.sleep(1);
         }
         return 0;
      }
   }

   private class ExercisePutTask implements HaltableTask{

      private AtomicBoolean halted = new AtomicBoolean(false);
      private Map<Integer,Integer> map;
      private BasicCache cache;

      public ExercisePutTask(Map<Integer, Integer> map, BasicCache cache){
         this.map = map;
         this.cache = cache;
      }

      @Override
      public void halt() {
         halted.set(true);
      }

      @Override
      public Integer call() throws Exception {
         Random random = new Random(System.nanoTime());
         while(!halted.get()) {
            try {
               int k = random.nextInt(Integer.MAX_VALUE);
               cache.put(k, 0);
               map.put(k,0);
               Thread.sleep(1);
            }catch (Exception e) {
               e.printStackTrace();
               // ignore
            }
         }
         return 0;
      }

   }

   private class ExerciseEventTask implements Callable<Integer> {

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
   public class ClusterListener {

      public ConcurrentMap<Integer, Integer> received = new ConcurrentHashMap<>();

      @CacheEntryCreated
      @CacheEntryModified
      @CacheEntryRemoved
      public void onCacheEvent(CacheEntryEvent event) {
         int value = (int) event.getValue();
         received.putIfAbsent(value, value);
      }

   }

}
