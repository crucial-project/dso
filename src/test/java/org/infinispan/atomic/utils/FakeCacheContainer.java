package org.infinispan.atomic.utils;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;

/**
 * @author Pierre Sutra
 */
public class FakeCacheContainer implements BasicCacheContainer {

   @Override
   public <K, V> BasicCache<K, V> getCache() {
      return new FakeCache();
   }

   @Override
   public <K, V> BasicCache<K, V> getCache(String cacheName) {
      throw new IllegalArgumentException("Invalid usage");
   }

   @Override
   public void start() {
      throw new IllegalArgumentException("Invalid usage");
   }

   @Override
   public void stop() {
      throw new IllegalArgumentException("Invalid usage");
   }
}
