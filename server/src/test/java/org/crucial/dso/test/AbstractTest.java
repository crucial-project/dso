package org.crucial.dso.test;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.crucial.dso.*;
import org.crucial.dso.utils.Context;
import org.crucial.dso.utils.ContextManager;
import org.crucial.dso.utils.ID;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Future;

import static org.crucial.dso.Factory.DSO_CACHE_NAME;
import static org.testng.Assert.assertTrue;

/**
 * @author Pierre Sutra
 */

public abstract class AbstractTest extends MultipleCacheManagersTest {

    protected static final CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
    protected static final int NCALLS = 5000;
    protected static final long MAX_ENTRIES = -1;
    protected static final int REPLICATION_FACTOR = 2;
    protected static final int NMANAGERS = 3;
    protected static final boolean PASSIVATION = false;
    protected static final String PERSISTENT_STORAGE_DIR = "/tmp/dso";

    @Test(groups = {"dso"})
    public void baseUsage() {

        BasicCacheContainer cacheManager = containers().iterator().next();
        BasicCache<Object, Object> cache = cacheManager.getCache(DSO_CACHE_NAME);
        Factory factory = Factory.forCache(cache);

        // 1 - basic call
        Set<String> set = factory.getInstanceOf(AtomicSet.class, "set");
        set.add("smthing");
        System.out.println(set.size());
        assert set.contains("smthing");
        assert set.size() == 1;

        factory.close();

    }

    @Test(groups = {"dso", "stress"})
    public void basePerformance() {

        BasicCacheContainer cacheManager = containers().iterator().next();
        BasicCache<Object, Object> cache = cacheManager.getCache(DSO_CACHE_NAME);
        Factory factory = Factory.forCache(cache);

        int f = 1; // multiplicative factor

        Map map = factory.getInstanceOf(AtomicMap.class, "map");

        long start = System.currentTimeMillis();
        for (int i = 0; i < NCALLS * f; i++) {
            map.containsKey("1");
        }

        System.out.println("op/sec:" + ((float) (NCALLS * f)) / ((float) (System.currentTimeMillis() - start)) * 1000);

    }

    @Test(groups = {"dso"})
    public void persistence() {

        assertTrue(containers().size() >= 2);

        Iterator<BasicCacheContainer> it = containers().iterator();

        BasicCacheContainer container1 = it.next();
        BasicCache<Object, Object> cache1 = container1.getCache(DSO_CACHE_NAME);
        Factory factory1 = Factory.forCache(cache1);

        BasicCacheContainer container2 = it.next();
        BasicCache<Object, Object> cache2 = container2.getCache(DSO_CACHE_NAME);
        Factory factory2 = Factory.forCache(cache2);

        Set set1, set2;

        // 0 - Base persistence
        set1 = factory1.getInstanceOf(AtomicSet.class, "persist1", false, false, true);
        set1.add("smthing");
        factory1.disposeInstanceOf(AtomicSet.class, "persist1");
        set1 = factory1.getInstanceOf(AtomicSet.class, "persist1", false, false, false);
        assert set1.contains("smthing");
        factory1.disposeInstanceOf(AtomicSet.class, "persist1");

        // 1 - Concurrent retrieval
        set1 = factory1.getInstanceOf(AtomicSet.class, "persist2");
        set1.add("smthing");
        set2 = factory2.getInstanceOf(AtomicSet.class, "persist2", false, false, false);
        assert set2.contains("smthing");
        factory1.disposeInstanceOf(AtomicSet.class, "persist2");
        factory2.disposeInstanceOf(AtomicSet.class, "persist2");

        // 2 - Serial storing then retrieval
        set1 = factory1.getInstanceOf(AtomicSet.class, "persist3");
        set1.add("smthing");
        factory1.disposeInstanceOf(AtomicSet.class, "persist3");
        set2 = factory2.getInstanceOf(AtomicSet.class, "persist3", false, false, false);
        assert set2.contains("smthing");
        factory1.disposeInstanceOf(AtomicSet.class, "persist3");
        factory2.disposeInstanceOf(AtomicSet.class, "persist3");

        // 3 - Re-creation
        set1 = factory1.getInstanceOf(AtomicSet.class, "persist4");
        set1.add("smthing");
        factory1.disposeInstanceOf(AtomicSet.class, "persist4");
        set2 = factory2.getInstanceOf(AtomicSet.class, "persist4", false, false, true);
        assert !set2.contains("smthing");
        factory2.disposeInstanceOf(AtomicSet.class, "persist4");

    }

    @Test(groups = {"dso"})
    public void baseCacheTest() {

        Iterator<BasicCacheContainer> it = containers().iterator();
        BasicCacheContainer container1 = it.next();
        BasicCache<Object, Object> cache1 = container1.getCache(DSO_CACHE_NAME);
        Factory factory1 = Factory.forCache(cache1, 1, false);

        Set set1, set2;

        // 0 - Base caching
        set1 = factory1.getInstanceOf(AtomicSet.class, "aset", false, false, true);
        set1.add("smthing");
        set2 = factory1.getInstanceOf(AtomicSet.class, "aset2", false, false, true);
        assert set1.contains("smthing");

        // 1 - Caching multiple instances of the same object
        set1 = factory1.getInstanceOf(AtomicSet.class, "aset3", false, false, true);
        set1.add("smthing");
        set2 = factory1.getInstanceOf(AtomicSet.class, "aset3", false, false, false);
        assert set1.contains("smthing");
        assert set2.contains("smthing");

    }

    @Test(groups = {"dso", "stress"})
    public void concurrentUpdates() throws Exception {

        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<Integer>> futures = new ArrayList<>();

        for (BasicCacheContainer manager : containers()) {
            Set set = Factory.forCache(manager.getCache(DSO_CACHE_NAME))
                    .getInstanceOf(AtomicSet.class, "concurrent");
            futures.add(service.submit(
                    new SetTask(set, NCALLS)));
        }

        long start = System.currentTimeMillis();
        Integer total = 0;
        for (Future<Integer> future : futures) {
            total += future.get();
        }
        System.out.println("Average time: " + (System.currentTimeMillis() - start));

        assert total == (NCALLS) : "obtained = " + total + "; expected = " + (NCALLS);

    }

    @Test(groups = {"dso"})
    public void multipleCreation() {

        assertTrue(containers().size() >= 2);

        Iterator<BasicCacheContainer> it = containers().iterator();

        BasicCacheContainer container1 = it.next();
        BasicCache<Object, Object> cache1 = container1.getCache(DSO_CACHE_NAME);
        Factory factory1 = Factory.forCache(cache1);

        BasicCacheContainer container2 = it.next();
        BasicCache<Object, Object> cache2 = container2.getCache(DSO_CACHE_NAME);
        Factory factory2 = Factory.forCache(cache2);

        int n = 100;
        for (int i = 0; i < n; i++) {
            List list = factory2.getInstanceOf(AtomicList.class, "list" + i);
            list.add(i);
        }

        for (int i = 0; i < n; i++) {
            List list = factory1.getInstanceOf(AtomicList.class, "list" + i);
            assert (list.get(0).equals(i)) : list.get(0);
        }

    }

    @Test(groups = {"dso"})
    public void baseComposition() {
        assert ShardedObject.class.isAssignableFrom(ShardedObject.class);

        Iterator<BasicCacheContainer> it = containers().iterator();
        BasicCacheContainer container1 = it.next();
        BasicCache<Object, Object> cache1 = container1.getCache(DSO_CACHE_NAME);
        Factory factory1 = Factory.forCache(cache1);

        ShardedObject object = factory1.getInstanceOf(ShardedObject.class, "o1");
        ShardedObject object2 = factory1.getInstanceOf(ShardedObject.class, "o2",
                false,
                false,
                true,
                "o2", object);
        ShardedObject object3 = object2.getShard();
        assert object3.equals(object);
    }


    @Test(groups = {"dso", "stress"}, enabled = false)
    public void baseElasticity() throws Exception {

        baseComposition();
        baseUsage();

        addContainer();
        persistence();
        baseComposition();
        baseUsage();

        deleteContainer();
        baseUsage();
        baseComposition();
    }

    @Test(groups = {"dso", "stress"}, enabled = false)
    public void advancedElasticity() throws Exception {

        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<Integer>> futures = new ArrayList<>();

        Set set = Factory.forCache(manager(0).getCache(DSO_CACHE_NAME)).getInstanceOf(AtomicSet.class, "elastic");
        futures.add(service.submit(
                new SetTask(set, NCALLS)));

        // elasticity
        Set<Future> completed = new HashSet<>();
        while (completed.size() != futures.size()) {
            Thread.sleep(3000);
            if (containers().size() == NMANAGERS) {
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

        assert total == (NCALLS) : "obtained = " + total + "; expected = " + (NCALLS);

    }

    @Test(groups = {"dso", "stress"})
    public void memoryUsage(){
        Iterator<BasicCacheContainer> it = containers().iterator();
        BasicCacheContainer container1 = it.next();
        BasicCache<Object, Object> cache1 = container1.getCache(DSO_CACHE_NAME);
        Factory factory1 = Factory.forCache(cache1);

        Map map = factory1.getInstanceOf(AtomicMap.class, "map");
        final int threads = 1;
        final int operations = 15;

        List<Future<Void>> futures = new ArrayList<>();
        ExecutorService service = Executors.newFixedThreadPool(threads);
        int content = 10000000;
        for (int i=0; i < threads; i++) {
            Future<Void> future = service.submit(() -> {
                Random random = new Random(System.nanoTime());
                for (int j= 0; j < operations; j++) {
                    int k = random.nextInt();
                    map.put(k, new byte[content]);
                    System.out.println(j + ":" + content/1000000 +"MB -> "+Runtime.getRuntime().totalMemory()/1000000+"MB");
                }
                return null;
            });
            futures.add(future);
        }
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    @Test(groups = {"dso"})
    public void idempotence() {

        Iterator<BasicCacheContainer> it = containers().iterator();
        BasicCacheContainer container1 = it.next();
        BasicCache<Object, Object> cache1 = container1.getCache(DSO_CACHE_NAME);
        Factory factory1 = Factory.forCache(cache1);

        SimpleObject object = factory1.getInstanceOf(SimpleObject.class,"idempotence");
        object.getCount(); // to open it.

        RandomBasedGenerator generator = null;

        generator = Generators.randomBasedGenerator(new Random(42));
        ContextManager.set(new Context(ID.threadID(), generator, Factory.forCache(this.manager(0).getCache(DSO_CACHE_NAME))));
        object.setField("a");

        generator = Generators.randomBasedGenerator(new Random(42));
        ContextManager.set(new Context(ID.threadID(), generator, Factory.forCache(this.manager(0).getCache(DSO_CACHE_NAME))));
        object.setField("a");

        assert object.getCount() == 1;

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
        Factory factory;
        for (EmbeddedCacheManager manager : cacheManagers) {
            factory = Factory.forCache(manager.getCache(DSO_CACHE_NAME));
            if (factory != null) factory.close();
        }
        for (BasicCacheContainer container : containers()) {
            factory = Factory.forCache(container.getCache(DSO_CACHE_NAME));
            factory.close();
        }
        super.destroy();
    }

    protected void assertOnAllCaches(Object key, String value) {
        for (Cache c : caches()) {
            Object realVal = c.get(key);
            if (value == null) {
                assert realVal == null : "Expecting [" + key + "] to equal [" + value + "] on cache " + c;
            } else {
                assert value.equals(realVal) : "Expecting [" + key + "] to equal [" + value + "] on cache " + c;
            }
        }
        // Allow some time for all ClusteredGetCommands to finish executing
        TestingUtil.sleepThread(1000);
    }

    public class SetTask implements Callable<Integer> {

        private int ncalls;
        private Set set;

        public SetTask(Set set, int n) {
            ncalls = n;
            this.set = set;
        }

        @Override
        public Integer call() {

            int ret = 0;
            for (int i = 0; i < ncalls; i++) {

                if (set.add(i)) {
                    ret++;
                }

            }

            return ret;

        }
    }

}
