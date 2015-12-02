package org.infinispan.atomic;

import org.infinispan.Cache;
import org.infinispan.atomic.object.Call;
import org.infinispan.atomic.object.CallInvoke;
import org.infinispan.atomic.object.Reference;
import org.infinispan.atomic.utils.AdvancedShardedObject;
import org.infinispan.atomic.utils.ShardedObject;
import org.infinispan.atomic.utils.SimpleObject;
import org.infinispan.atomic.utils.SimpleShardedObject;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.marshall.core.JBossMarshaller;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Pierre Sutra
 */

@CleanupAfterMethod
@Test(testName = "AtomicObjectFactoryAbstractTest", groups = "unit", enabled = false)
public abstract class AtomicObjectFactoryAbstractTest extends MultipleCacheManagersTest {

   protected static Log log = LogFactory.getLog(AtomicObjectFactoryAbstractTest.class);
   protected static final CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
   protected static int NCALLS = 1000;
   protected static long MAX_ENTRIES = Long.MAX_VALUE;

   private final int REPLICATION_FACTOR = 2;
   public int getReplicationFactor(){
      return REPLICATION_FACTOR;
   }

   private int NMANAGERS = 3;
   public int getNumberOfManagers() {
      return NMANAGERS;
   }

   @Test(enabled = true)
   public void basePropertiesTest() throws Exception {

      // 0 - validate cache atomicity
      for(int i=0; i<100; i++) {
         UUID uuid = UUID.randomUUID();
         container(0).getCache().put(uuid, uuid);
         assert container(0).getCache().get(uuid).equals(uuid);
      }
   }

   @Test
   public void baseUsageTest() throws Exception {

      BasicCacheContainer cacheManager = containers().iterator().next();
      BasicCache<Object, Object> cache = cacheManager.getCache();
      AtomicObjectFactory factory = AtomicObjectFactory.forCache(cache);

      // 1 - basic call
      Set<String> set = factory.getInstanceOf(HashSet.class, "set");
      set.add("smthing");
      assert set.contains("smthing");
      assert set.size() == 1;

      // 2 - proxy marshalling
      Marshaller marshaller = new JBossMarshaller();
      assert marshaller.objectFromByteBuffer((marshaller.objectToByteBuffer(set))) instanceof Reference;

   }

   @Test(enabled = true)
   public void basePerformance() throws Exception{

      BasicCacheContainer cacheManager = containers().iterator().next();
      BasicCache<Object, Object> cache = cacheManager.getCache();
      AtomicObjectFactory factory = AtomicObjectFactory.forCache(cache);

      int f = 1; // multiplicative factor

      Map map = factory.getInstanceOf(HashMap.class, "map");

      long start = System.currentTimeMillis();
      for (int i = 0; i < NCALLS * f; i++) {
         map.containsKey("1");
      }

      System.out.println("op/sec:" + ((float) (NCALLS * f)) / ((float) (System.currentTimeMillis() - start)) * 1000);

   }

   @Test
   public void persistenceTest() throws Exception {

      assertTrue(containers().size() >= 2);

      Iterator<BasicCacheContainer> it = containers().iterator();

      BasicCacheContainer container1 = it.next();
      BasicCache<Object, Object> cache1 = container1.getCache();
      AtomicObjectFactory factory1 = AtomicObjectFactory.forCache(cache1);

      BasicCacheContainer container2 = it.next();
      BasicCache<Object, Object> cache2 = container2.getCache();
      AtomicObjectFactory factory2 = AtomicObjectFactory.forCache(cache2);

      HashSet set1, set2;

      // 0 - Base persistence
      set1 = factory1.getInstanceOf(HashSet.class, "persist1", false, true);
      set1.add("smthing");
      factory1.disposeInstanceOf(HashSet.class, "persist1");
      set1 = factory1.getInstanceOf(HashSet.class, "persist1", false, false);
      assert set1.contains("smthing");
      factory1.disposeInstanceOf(HashSet.class, "persist1");

      // 1 - Concurrent retrieval
      set1 = factory1.getInstanceOf(HashSet.class, "persist2");
      set1.add("smthing");
      set2 = factory2.getInstanceOf(HashSet.class, "persist2", false, false);
      assert set2.contains("smthing");
      factory1.disposeInstanceOf(HashSet.class, "persist2");
      factory2.disposeInstanceOf(HashSet.class, "persist2");

      // 2 - Serial storing then retrieval
      set1 = factory1.getInstanceOf(HashSet.class, "persist3");
      set1.add("smthing");
      factory1.disposeInstanceOf(HashSet.class, "persist3");
      set2 = factory2.getInstanceOf(HashSet.class, "persist3", false, false);
      assert set2.contains("smthing");
      factory1.disposeInstanceOf(HashSet.class, "persist3");
      factory2.disposeInstanceOf(HashSet.class, "persist3");

      // 3 - Re-creation
      set1 = factory1.getInstanceOf(HashSet.class, "persist4");
      set1.add("smthing");
      factory1.disposeInstanceOf(HashSet.class, "persist4");
      set2 = factory2.getInstanceOf(HashSet.class, "persist4", false, true);
      assert !set2.contains("smthing");
      factory2.disposeInstanceOf(HashSet.class, "persist4");

   }

   @Test
   public void baseReadOptimizationTest() throws Exception {
      SimpleObject object = new SimpleObject();
      object.setField("something");
      String field = object.getField();
      assert field.equals("something");
      BasicCache cache = container(0).getCache();
      Call lastCall = (Call) cache.get(new Reference<>(SimpleObject.class, "test"));
      assertTrue(lastCall != null);
      assertEquals(lastCall.getClass(), CallInvoke.class);
      assert ((CallInvoke) lastCall).method.equals("setField");
   }

   @Test
   public void performanceReadOptimizationTest() throws Exception {

      int f = 10; // multiplicative factor
      SimpleObject object = new SimpleObject("performance");

      long start = System.currentTimeMillis();
      for(int i=0; i<NCALLS*f;i++){
         object.setField(Integer.toString(i));
      }
      System.out.println("op/sec:"+((float)(NCALLS*f))/((float)(System.currentTimeMillis() - start))*1000);

      start = System.currentTimeMillis();
      for(int i=0; i<NCALLS*f;i++){
         object.getField();
      }
      System.out.println("op/sec:" + ((float) (NCALLS * f)) / ((float) (System.currentTimeMillis() - start)) * 1000);

   }

   @Test
   public void baseCacheTest() throws Exception {

      Iterator<BasicCacheContainer> it = containers().iterator();
      BasicCacheContainer container1 = it.next();
      BasicCache<Object, Object> cache1 = container1.getCache();
      AtomicObjectFactory factory1 = AtomicObjectFactory.forCache(cache1, 1);

      HashSet set1, set2;

      // 0 - Base caching
      set1 = factory1.getInstanceOf(HashSet.class, "aset", false, true);
      set1.add("smthing");
      set2 = factory1.getInstanceOf(HashSet.class, "aset2", false, true);
      assert set1.contains("smthing");

      // 1 - Caching multiple instances of the same object
      set1 = factory1.getInstanceOf(HashSet.class, "aset3", false, true);
      set1.add("smthing");
      set2 = factory1.getInstanceOf(HashSet.class, "aset3", false, false);
      assert set1.contains("smthing");
      assert set2.contains("smthing");

   }

   @Test
   public void distributedUsageTest() throws Exception {

      ExecutorService service = Executors.newCachedThreadPool();
      List<Future<Integer>> futures = new ArrayList<>();

      for (BasicCacheContainer manager : containers()) {
         BasicCache<Object, Object> cache = manager.getCache();
         futures.add(service.submit(
               new ExerciseAtomicSetTask(
                     AtomicObjectFactory.forCache(cache), "distt", NCALLS)));
      }

      long start = System.currentTimeMillis();
      Integer total = 0;
      for (Future<Integer> future : futures) {
         total += future.get();
      }
      System.out.println("Average time: " + (System.currentTimeMillis() - start));

      assert total == (NCALLS) : "obtained = " + total + "; espected = " + (NCALLS);

   }

   @Test
   public void distributedCacheTest() throws Exception {

      assertTrue(containers().size() >= 2);

      int numMaps = 2;

      Iterator<BasicCacheContainer> it = containers().iterator();

      BasicCacheContainer container1 = it.next();
      BasicCache<Object, Object> cache1 = container1.getCache();
      AtomicObjectFactory factory1 = AtomicObjectFactory.forCache(cache1);

      BasicCacheContainer container2 = it.next();
      BasicCache<Object, Object> cache2 = container2.getCache();
      AtomicObjectFactory factory2 = AtomicObjectFactory.forCache(cache2);

      for (int i = 0; i < numMaps; i++) {
         for (int j = 0; j <= i; j++) {
            Map map2 = factory2.getInstanceOf(HashMap.class, "map" + i);
            map2.put(j, i);
         }
      }

      for (int i = 0; i < numMaps; i++) {
         for (int j = 0; j <= i; j++) {
            Map map2 = factory1.getInstanceOf(HashMap.class, "map" + i);
            assertTrue(map2.get(j).equals(i));
         }
      }

   }

   @Test
   public void baseAspectJTest() throws Exception {

      // 1 - constructor
      SimpleObject object = new SimpleObject();
      String field = object.getField();
      assert field.equals("test");

      // 2 - constructor w. arguments
      SimpleObject object1 = new SimpleObject("test2");
      assert object1.getField().equals("test2");

      // 3 - equals()
      AdvancedShardedObject advancedShardedObject = new AdvancedShardedObject();
      assert advancedShardedObject.equals(advancedShardedObject.getSelf());

   }

   @Test(enabled = true)
   public void baseCompositionTest() throws Exception {
      SimpleShardedObject object = new SimpleShardedObject();
      SimpleShardedObject object2 = new SimpleShardedObject(object);
      ShardedObject object3 = object2.getShard();
      assert object3.equals(object);

      List<SimpleObject> list = new ArrayList<>();
      Random random = new Random(System.currentTimeMillis());
      for(int i=0; i<10; i++) {
         list.add(new SimpleObject(Integer.toString(random.nextInt(10))));
      }
      for(SimpleObject simpleObject1 : list){
         for(SimpleObject simpleObject2 : list){
            if (simpleObject1.equals(simpleObject2))
               assert simpleObject1.getField().equals(simpleObject2.getField());
         }
      }

   }

   @Test(enabled = true)
   public void advancedCompositionTest() throws Exception {
      AdvancedShardedObject object1 = new AdvancedShardedObject();
      AdvancedShardedObject object2 = new AdvancedShardedObject(object1);

      assert object2.getShard().equals(object1);
      assert object1.flipValue();
      assert !((AdvancedShardedObject) object2.getShard()).flipValue();
      assert object2.flipValue();

      // TODO improve tests on static fields
      List<AdvancedShardedObject> rlist = object2.getList();
      rlist.clear();
      object1.addSelf();
      assert rlist.get(0) instanceof AdvancedShardedObject;
      assert rlist.get(0).equals(object1) :  rlist.get(0);
   }

   @Test(enabled = false)
   public void baseElasticity() throws Exception {
      assertTrue(containers().size() >= 2);

      persistenceTest();
      advancedCompositionTest();

      addContainer();
      persistenceTest();
      advancedCompositionTest();

      deleteContainer();
      persistenceTest();
      advancedCompositionTest();
   }

   @Test(enabled = false)
   public void advancedElasticity() throws Exception {

      ExecutorService service = Executors.newCachedThreadPool();
      List<Future<Integer>> futures = new ArrayList<>();

      for (BasicCacheContainer manager : containers()) {
         BasicCache<Object, Object> cache = manager.getCache();
         futures.add(service.submit(
               new ExerciseAtomicSetTask(
                     AtomicObjectFactory.forCache(cache), "elastic", NCALLS)));
      }

      waitForClusterToForm();

      // elasticity
      Set<Future> completed = new HashSet<>();
      Random random = new Random();
      while (completed.size() != futures.size()) {
         Thread.sleep(2000);
         boolean action = true;
         if (containers().size() > REPLICATION_FACTOR)
            action = (random.nextBoolean());
         if (action) {
            addContainer();
         } else {
            deleteContainer();
         }
         for (Future<Integer> future : futures) {
            if (future.isDone())
               completed.add(future);
         }
      }

      Integer total = 0;
      for (Future<Integer> future : futures) {
         total += future.get();
      }

      assert total == (NCALLS) : "obtained = " + total + "; espected = " + (NCALLS);

   }

   //
   // Interface
   //

   public abstract BasicCacheContainer container(int i);

   public abstract Collection<BasicCacheContainer> containers();

   public abstract boolean addContainer();

   public abstract boolean deleteContainer();

   //
   // Helpers
   //

   @Override
   @AfterClass(alwaysRun = true)
   protected void destroy() {
      for (BasicCacheContainer container : containers()) {
         AtomicObjectFactory.forCache(container.getCache()).close();
      }
      super.destroy();
   }

   protected void assertOnAllCaches(Object key, String value) {
      for (Cache c : caches()) {
         Object realVal = c.get(key);
         if (value == null) {
            assert realVal == null : "Expecting [" + key + "] to equal [" + value + "] on cache " + c.toString();
         } else {
            assert value.equals(realVal) : "Expecting [" + key + "] to equal [" + value + "] on cache " + c.toString();
         }
      }
      // Allow some time for all ClusteredGetCommands to finish executing
      TestingUtil.sleepThread(1000);
   }

   public static class ExerciseAtomicSetTask implements Callable<Integer> {

      private String name;
      private int ncalls;
      private Set set;
      private AtomicObjectFactory factory;

      public ExerciseAtomicSetTask(AtomicObjectFactory f, String name, int n) {
         this.name = name;
         factory = f;
         ncalls = n;
      }

      @Override
      public Integer call() throws Exception {

         int ret = 0;

         for (int i = 0; i < ncalls; i++) {

            if (set == null)
               set = factory.getInstanceOf(HashSet.class, name);

            Object r = set.add(i);
            assert r != null;

            // if successful, persist the object
            if (r != null && (boolean) r) {
               ret++;
               factory.disposeInstanceOf(HashSet.class, name);
               set = null;
            }
         }

         return ret;

      }
   }

}
