package org.infinispan.atomic;

import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;
import static org.testng.Assert.assertEquals;

/**
 * @author Pierre Sutra
 */
@Test(testName = "AtomicObjectFactoryRemoteTest", groups = "unit")
public class AtomicObjectFactoryRemoteTest extends AtomicObjectFactoryAbstractTest{

   private static List<HotRodServer> servers = new ArrayList<>();
   private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();
   private static ConfigurationBuilder defaultBuilder;

   @Override 
   public BasicCacheContainer container(int i) {
      return remoteCacheManagers.get(i);
   }

   @Override 
   public Collection<BasicCacheContainer> containers() {
      return remoteCacheManagers;
   }

   @Override
   public boolean addContainer() {
      int index = servers.size();

      // embedded cache manager
      if (MAX_ENTRIES!=Long.MAX_VALUE) {
         defaultBuilder.eviction().maxEntries(MAX_ENTRIES);
         defaultBuilder.persistence().clearStores();
         defaultBuilder.persistence().addSingleFileStore().location("/tmp/aof-storage/" + index);
      }
      addClusterEnabledCacheManager(defaultBuilder).getCache();
      waitForClusterToForm();

      // hotrod server
      HotRodServer server = HotRodTestingUtil.startHotRodServer(
            manager(index),
            11222+index);
      FilterConverterFactory factory = new FilterConverterFactory();
      server.addCacheEventFilterConverterFactory(FilterConverterFactory.FACTORY_NAME, factory);
      server.startDefaultCache();
      servers.add(server);

      // remote manager
      RemoteCacheManager manager = new RemoteCacheManager(
            new org.infinispan.client.hotrod.configuration.ConfigurationBuilder()
                  .addServers(server.getHost()+":"+server.getPort())
                  .marshaller((Marshaller) null)
                  .build());
      remoteCacheManagers.add(manager);

      System.out.println("Node " + manager+ " added.");
      return true;
   }

   @Override
   public  boolean deleteContainer() {
      if (servers.size()==0) return false;
      int index = servers.size() - 1;

      // remote manager
      remoteCacheManagers.get(index).stop();
      remoteCacheManagers.remove(index);

      // hotrod server
      servers.get(index).stop();
      servers.remove(index);

      // embedded cache manager
      BasicCacheContainer manager = cacheManagers.get(index);
      cacheManagers.get(index).stop();
      cacheManagers.remove(index);

      waitForClusterToForm();
      System.out.println("Node " + manager+ " deleted.");

      return true;
   }


   @Override 
   protected void createCacheManagers() throws Throwable {
      createDefaultBuilder();

      for (int j = 0; j < getNumberOfManagers(); j++) {
         addContainer();
      }

      // Verify that default caches are started.
      for (int j = 0; j < getNumberOfManagers(); j++) {
         blockUntilCacheStatusAchieved(
               manager(j).getCache(), ComponentStatus.RUNNING, 10000);
      }

      waitForClusterToForm();

      assertEquals(manager(0).getTransport().getMembers().size(),getNumberOfManagers());

      AtomicObjectFactory.forCache(container(0).getCache());
   }

   // Helpers

   private void createDefaultBuilder() {
      defaultBuilder = getDefaultClusteredCacheConfig(CACHE_MODE,false);
      defaultBuilder
            .clustering().cacheMode(CacheMode.DIST_SYNC).hash().numOwners(getReplicationFactor())
            .locking().useLockStriping(false)
            .compatibility().enable();
      defaultBuilder.clustering().stateTransfer()
            .awaitInitialTransfer(true)
            .timeout(1000000)
            .fetchInMemoryState(true);
   }

}
