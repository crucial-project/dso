package org.infinispan.atomic;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pierre Sutra
 */
@Test(testName = "AtomicObjectFactoryRemoteRealTest", groups = "unit", enabled = false)
public class AtomicObjectFactoryRemoteRealTest extends AtomicObjectFactoryAbstractTest {

   private static List<BasicCacheContainer> basicCacheContainers = new ArrayList<>();

   @Override 
   public BasicCacheContainer container(int i) {
      return basicCacheContainers.get(i% basicCacheContainers.size());
   }

   @Override 
   public Collection<BasicCacheContainer> containers() {
      return basicCacheContainers;
   }

   @Override
   public boolean addContainer() {
      return false;
   }

   @Override
   public boolean deleteContainer() {
      return false;
   }

   @Override 
   protected void createCacheManagers() throws Throwable {
      
      for(String server : servers()) {
         String host = server.split(":")[0];
         int port = Integer.valueOf(server.split(":")[1]);
         org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
               = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
         cb.pingOnStartup(true)
               .addServer()
               .host(host)
               .port(port);
         RemoteCacheManager manager= new RemoteCacheManager(cb.build());
         manager.start();
         manager.getCache().clear();
         basicCacheContainers.add(manager);
      }

      this.cleanup = null;
      AtomicObjectFactory.forCache(basicCacheContainers.get(0).getCache());
   }

   @Override
   protected void clearContent() throws Throwable {
      for(BasicCacheContainer container : basicCacheContainers) {
         container.getCache().clear();
      }
   }

   protected String[] servers () {
      return new String[]{"127.0.0.1:11222","127.0.0.1:11223"};
   }

}
