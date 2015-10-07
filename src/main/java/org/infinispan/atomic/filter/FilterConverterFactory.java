package org.infinispan.atomic.filter;

import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverterFactory;
import org.infinispan.notifications.cachelistener.filter.NamedFactory;

/**
 * @author Pierre Sutra
 */
@NamedFactory(name ="org.infinispan.atomic.filter.CacheEventFilterFactory")
public class FilterConverterFactory implements CacheEventFilterConverterFactory {

   public static final String FACTORY_NAME = "org.infinispan.atomic.filter.CacheEventFilterFactory";

   private ObjectFilterConverter objectFilterConverter = new ObjectFilterConverter();

   @Override
   public CacheEventFilterConverter getFilterConverter(Object[] params) {
      return new CompositeCacheEventFilterConverter<>(
            new ListenerBasedCacheEventFilterConverter(params),
            objectFilterConverter);
   }

}
