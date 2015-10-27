package org.infinispan.atomic;

import org.infinispan.Cache;
import org.infinispan.atomic.object.Call;
import org.infinispan.atomic.object.CallInvoke;
import org.infinispan.atomic.object.Reference;
import org.infinispan.atomic.utils.SimpleObject;
import org.infinispan.atomic.utils.SimpleShardedObject;
import org.infinispan.atomic.utils.SimpleShardedObjectInterface;
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

   protected static int NMANAGERS = 3;
   protected static final int REPLICATION_FACTOR = 1;
   protected static final CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
   protected static final boolean USE_TRANSACTIONS = false;
   protected static int NCALLS = 100;

   @Test
   public void baseUsageTest() throws  Exception{

      BasicCacheContainer cacheManager = containers().iterator().next();
      BasicCache<Object,Object> cache = cacheManager.getCache();
      AtomicObjectFactory factory = new AtomicObjectFactory(cache);
      
      // 1 - basic call
      Set<String> set = factory.getInstanceOf(HashSet.class, "set");
      set.add("smthing");
      assert set.contains("smthing");
      assert set.size()==1;
      
      // 2 - proxy marshalling
      Marshaller marshaller = new JBossMarshaller();
      assert marshaller.objectFromByteBuffer((marshaller.objectToByteBuffer(set))) instanceof Reference;

   }

   @Test(enabled = true)
   public void basePerformanceTest() throws Exception{

      BasicCacheContainer cacheManager = containers().iterator().next();
      BasicCache<Object,Object> cache = cacheManager.getCache();
      AtomicObjectFactory factory = new AtomicObjectFactory(cache);
      
      int f = 1; // multiplicative factor

      Map map = factory.getInstanceOf(HashMap.class, "map");

      long start = System.currentTimeMillis();
      for(int i=0; i<NCALLS*f;i++){
         // cache.put("1","1");
         map.containsKey("1");
      }

      System.out.println("op/sec:"+((float)(NCALLS*f))/((float)(System.currentTimeMillis() - start))*1000);

   }

   @Test
   public void persistenceTest() throws Exception {

      assertTrue(containers().size()>=2);

      Iterator<BasicCacheContainer> it = containers().iterator();
      
      BasicCacheContainer container1 = it.next();
      BasicCache<Object,Object> cache1 = container1.getCache();
      AtomicObjectFactory factory1 = new AtomicObjectFactory(cache1);
      
      BasicCacheContainer container2 = it.next();
      BasicCache<Object,Object> cache2 = container2.getCache();
      AtomicObjectFactory factory2 = new AtomicObjectFactory(cache2);

      HashSet set1, set2;

      // 0 - Base persistence
      set1 = factory1.getInstanceOf(HashSet.class, "persist", false, null, true);
      set1.add("smthing");
      factory1.disposeInstanceOf(HashSet.class, "persist", true);
      set1 = factory1.getInstanceOf(HashSet.class, "persist", false, null, false);
      assert set1.contains("smthing");

      // 1 - Concurrent retrieval
      set1 = factory1.getInstanceOf(HashSet.class, "persist1");
      set1.add("smthing");
      set2 = factory2.getInstanceOf(HashSet.class, "persist1", false, null, false);
      assert set2.contains("smthing");

      // 2 - Serial storing then retrieval
      set1 = factory1.getInstanceOf(HashSet.class, "persist2");
      set1.add("smthing");
      factory1.disposeInstanceOf(HashSet.class,"persist2",true);
      set2 = factory2.getInstanceOf(HashSet.class, "persist2", false, null, false);
      assert set2.contains("smthing");

      // 3 - Re-creation
      set1 = factory1.getInstanceOf(HashSet.class, "persist3");
      set1.add("smthing");
      factory1.disposeInstanceOf(HashSet.class,"persist3",true);
      set2 = factory2.getInstanceOf(HashSet.class, "persist3", false, null, true);
      assert !set2.contains("smthing");

   }
   
   @Test
   public void baseReadOptimizationTest() throws Exception{
      SimpleObject object = new SimpleObject();
      object.setField("something");
      String field = object.getField();
      assert field.equals("something");
      Call lastCall = (Call) container(0).getCache().get(new Reference<>(SimpleObject.class,"test"));
      assertTrue(lastCall!=null);
      assertEquals(lastCall.getClass(),CallInvoke.class);
      assert ((CallInvoke)lastCall).method.equals("setField");
   }
   
   @Test
   public void performanceReadOptimizationTest() throws Exception{

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
      System.out.println("op/sec:"+((float)(NCALLS*f))/((float)(System.currentTimeMillis() - start))*1000);

   } 
   
   @Test
   public void baseCacheTest() throws Exception{

      Iterator<BasicCacheContainer> it = containers().iterator();
      BasicCacheContainer container1 = it.next();
      BasicCache<Object,Object> cache1 = container1.getCache();
      AtomicObjectFactory factory1 = new AtomicObjectFactory(cache1,1);

      HashSet set1, set2;

      // 0 - Base caching
      set1 = factory1.getInstanceOf(HashSet.class, "aset", false, null, true);
      set1.add("smthing");
      set2 = factory1.getInstanceOf(HashSet.class, "aset2", false, null, true);
      assert set1.contains("smthing");
      
      // 1 - Caching multiple instances of the same object
      set1 = factory1.getInstanceOf(HashSet.class, "aset3", false, null, true);
      set1.add("smthing");
      set2 = factory1.getInstanceOf(HashSet.class, "aset3", false, null, false);
      assert set1.contains("smthing");
      assert set2.contains("smthing");

   }

   @Test
   public void distributedUsageTest() throws Exception {

      ExecutorService service = Executors.newCachedThreadPool();
      List<Future<Integer>> futures = new ArrayList<>();

      for(BasicCacheContainer manager: containers()){
         BasicCache<Object,Object> cache = manager.getCache();
         futures.add(service.submit(
               new ExerciseAtomicSetTask(
                     new AtomicObjectFactory(cache), NCALLS)));
      }

      long start = System.currentTimeMillis();
      Integer total = 0;
      for(Future<Integer> future : futures){
         total += future.get();
      }
      System.out.println("Average time: "+(System.currentTimeMillis()-start));

      assert total == (NCALLS) : "obtained = "+total+"; espected = "+ (NCALLS);

   }
   
   @Test
   public void distributedCacheTest() throws Exception {

      assertTrue(containers().size()>=2);

      Iterator<BasicCacheContainer> it = containers().iterator();

      BasicCacheContainer container1 = it.next();
      BasicCache<Object,Object> cache1 = container1.getCache();
      AtomicObjectFactory factory1 = new AtomicObjectFactory(cache1);

      BasicCacheContainer container2 = it.next();
      BasicCache<Object,Object> cache2 = container2.getCache();
      AtomicObjectFactory factory2 = new AtomicObjectFactory(cache2);

      for (int i = 0; i < 10; i++) {
         for (int j = 0; j <= i; j++) {
            Map map2 = factory2.getInstanceOf(HashMap.class, "map"+i);
            map2.put(j,i);
         }
      }

      for (int i = 0; i < 10; i++) {
         for (int j = 0; j <= i; j++) {
            Map map2 = factory1.getInstanceOf(HashMap.class, "map"+i);
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

   }
   
   @Test
   public void baseShardTest() throws Exception {
      SimpleShardedObject object = new SimpleShardedObject();
      SimpleShardedObject object2 = new SimpleShardedObject(object);
      SimpleShardedObjectInterface object3 = object2.getShard();
      assert object3.equals(object);
   }


   @Test(enabled = false)
   public void baseScalability() throws Exception {
      assertTrue(containers().size()>=2);
      baseUsageTest();
      deleteContainer();
      baseUsageTest();
      addContainer();
      baseCacheTest();
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
   protected void destroy(){
      for (BasicCacheContainer container : containers()) {
         AtomicObjectFactory.forCache(container.getCache()).close();
      }
      super.destroy();
   }

   protected void assertOnAllCaches(Object key, String value) {
      for (Cache c : caches()) {
         Object realVal = c.get(key);
         if (value == null) {
            assert realVal == null : "Expecting [" + key + "] to equal [" + value + "] on cache "+ c.toString();
         } else {
            assert value.equals(realVal) : "Expecting [" + key + "] to equal [" + value + "] on cache "+c.toString();
         }
      }
      // Allow some time for all ClusteredGetCommands to finish executing
      TestingUtil.sleepThread(1000);
   }

   public static class ExerciseAtomicSetTask implements Callable<Integer>{

      private static final String name="aset";

      private int ncalls;
      private Set set;
      private AtomicObjectFactory factory;

      public ExerciseAtomicSetTask(AtomicObjectFactory f, int n){
         factory = f;
         ncalls = n;
      }

      @Override
      public Integer call() throws Exception {
         
         int ret = 0;
         
         for(int i=0; i<ncalls;i++){
            
            if (set==null) 
               set = factory.getInstanceOf(HashSet.class, name);
            
            boolean r = set.add(i);
            
            // if successful, persist the object
            if(r){
               ret ++;
               factory.disposeInstanceOf(HashSet.class, name, true);
               set = null;
            }
         }
         
         return  ret;
         
      }
   }

}
