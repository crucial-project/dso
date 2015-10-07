package org.infinispan.atomic;

import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.AbstractCacheTest;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

/**
 * @author Pierre Sutra
 * @since 7.0
 */
@Test(testName = "AtomicObjectFactoryTest", groups = "unit")
public class AtomicObjectFactoryTest extends AtomicObjectFactoryAbstractTest {

   private static ConfigurationBuilder defaultConfigurationBuilder;
   private static List<Cache<Object,Object>> caches;

   @Override 
   public BasicCacheContainer container(int i) {
      return manager(i);
   }

   @Override 
   public Collection<BasicCacheContainer> containers() {
      return (Collection)getCacheManagers();
   }

   @Override
   public boolean addContainer() {
      EmbeddedCacheManager cm = addClusterEnabledCacheManager(defaultConfigurationBuilder);
      caches.add(cm.getCache());
      return true;
   }

   @Override
   public boolean deleteContainer() {
      assert caches.size()==containers().size();
      if (caches.size()==0)
         return false;
      BasicCacheContainer container = container(containers().size() - 1);
      container.stop();
      containers().remove(container);
      caches.remove(caches.size()-1);
      return true;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      createConfigurationBuilder();
      caches = createClusteredCaches(NMANAGERS, defaultConfigurationBuilder);
      AtomicObjectFactory.forCache(cache(0));
   }

   //
   // HELPERS
   //


   private void createConfigurationBuilder() {
      defaultConfigurationBuilder
            = AbstractCacheTest.getDefaultClusteredCacheConfig(CACHE_MODE, USE_TRANSACTIONS);
      defaultConfigurationBuilder
            .clustering().hash().numOwners(REPLICATION_FACTOR)
            .locking().useLockStriping(false);

   }

}

