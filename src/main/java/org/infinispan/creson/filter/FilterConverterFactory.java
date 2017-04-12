package org.infinispan.creson.filter;

import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverterFactory;
import org.infinispan.notifications.cachelistener.filter.NamedFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;


/**
 * @author Pierre Sutra
 */
@NamedFactory(name ="org.infinispan.atomic.filter.CacheEventFilterFactory")
public class FilterConverterFactory implements CacheEventFilterConverterFactory {

   public static final String FACTORY_NAME = "org.infinispan.atomic.filter.CacheEventFilterFactory";

   private static Log log = LogFactory.getLog(ListenerBasedCacheEventFilterConverter.class);

   private ObjectFilterConverter objectFilterConverter = new ObjectFilterConverter();

   @Override
   public CacheEventFilterConverter getFilterConverter(Object[] params) {
      log.trace(this+" getFilterConverter()");
      return new CompositeCacheEventFilterConverter<>(
            new ListenerBasedCacheEventFilterConverter(params),
            objectFilterConverter);
   }

   @Override
   public String toString(){
      return "FilterConverterFactory";
   }

}
