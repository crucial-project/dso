package org.infinispan.atomic.filter;

import org.infinispan.Cache;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheAware;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.EventType;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pierre Sutra
 */
public class CompositeCacheEventFilterConverter<K, V, C> extends AbstractCacheEventFilterConverter<K, V, C>
      implements Serializable, CacheAware {

   private static ConcurrentHashMap<Cache, ObjectFilterConverter> registry
         = new ConcurrentHashMap<>();

   private final CacheEventConverter<? super K, ? super V,? super C>[] converters;

   public CompositeCacheEventFilterConverter(CacheEventConverter<? super K, ? super V, ? super C>... converters) {
      this.converters= converters;
   }

   @Override
   public void setCache(Cache cache) {
      for (int i=0; i <converters.length; i++) {
         CacheEventConverter<? super K, ? super V, ? super C> converter = converters[i];
         if (converter instanceof ObjectFilterConverter) {
            ((CacheAware) converter).setCache(cache);
            if (!registry.contains(cache)) {
               registry.putIfAbsent(cache, (ObjectFilterConverter) converter);
            }
            converters[i] = (CacheEventConverter<? super K, ? super V, ? super C>) registry.get(cache);
         }
      }
   }

   @Override
   public C filterAndConvert(K key, V oldValue, Metadata oldMetadata, V newValue, Metadata newMetadata,
         EventType eventType) {
      C ret = null;
      for (CacheEventConverter<? super K, ? super V, ? super C> converter : converters) {
         ret = (C) converter.convert(key, oldValue, oldMetadata, newValue, newMetadata, eventType);
         if (ret == null)
            break;
      }
      return ret;
   }
}