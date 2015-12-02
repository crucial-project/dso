package org.infinispan.atomic.filter;

import org.infinispan.Cache;
import org.infinispan.atomic.object.Call;
import org.infinispan.atomic.object.Reference;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheAware;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pierre Sutra
 */
public class CompositeCacheEventFilterConverter<K, V, C> extends AbstractCacheEventFilterConverter<K, V, C>
      implements Serializable, CacheAware {

   private static final Log log = LogFactory.getLog(CompositeCacheEventFilterConverter.class);

   private static final ConcurrentHashMap<Cache, ObjectFilterConverter> registry
         = new ConcurrentHashMap<>();

   private final CacheEventConverter<? super K, ? super V,? super C>[] converters;

   public CompositeCacheEventFilterConverter(CacheEventConverter<? super K, ? super V, ? super C>... converters) {
      this.converters = converters;
   }

   @Override
   public void setCache(Cache cache) {
      for (int i=0; i <converters.length; i++) {
         CacheEventConverter<? super K, ? super V, ? super C> converter = converters[i];
         // FIXME there should be no marshalling of FC but instead a factory call
         if (converter instanceof ObjectFilterConverter) {
            if (!registry.containsKey(cache)) {
               log.info("Installing filter-converter on "+cache);
               ((CacheAware) converter).setCache(cache);
               registry.putIfAbsent(cache, (ObjectFilterConverter) converter);
            }
            converters[i] = (CacheEventConverter<? super K, ? super V, ? super C>) registry.get(cache);
         }
      }
   }

   @Override
   public C filterAndConvert(K key, V oldValue, Metadata oldMetadata, V newValue, Metadata newMetadata,
         EventType eventType) {

      if (log.isTraceEnabled()) log.trace(this+" filterAndConvert() "+newValue+" ("+eventType.getType()+")");

      if ( !(key instanceof Reference)
            || ((newValue != null) && !(newValue instanceof Call))
            || ((oldValue != null) && !(oldValue instanceof Call)) ) {
         if (log.isTraceEnabled())
            log.trace(this + " trashing (" + key + "," + newValue);
         return null;

      }

      C ret = null;
      assert converters.length==2;
      for (CacheEventConverter<? super K, ? super V, ? super C> converter : converters) {
         ret = (C) converter.convert(key, oldValue, oldMetadata, newValue, newMetadata, eventType);
         if (ret == null)
            break;
      }

      return ret;
   }

   @Override
   public String toString(){
      return "CompositeCacheEventFilterConverter";
   }

}