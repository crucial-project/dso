package org.infinispan.atomic.filter;

import org.infinispan.atomic.object.Call;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class ListenerBasedCacheEventFilterConverter<K,V> extends AbstractCacheEventFilterConverter<K,V,Object>
      implements Serializable{

   private static Log log = LogFactory.getLog(ListenerBasedCacheEventFilterConverter.class);

   private UUID listenerID;
   
   public ListenerBasedCacheEventFilterConverter(Object[] parameters){
      assert (parameters.length==1);
      listenerID = (UUID) parameters[0];
   }

   @Override
   public String toString(){
      return "ListenerBasedCacheEventFilterConverter["+ listenerID +"]";
   }

   @Override 
   public Object filterAndConvert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
         Metadata newMetadata, EventType eventType) {

      if (log.isTraceEnabled()) log.trace(this + " - filterAndConvert()");

      if (eventType.isPreEvent()) {
         if (log.isTraceEnabled()) log.trace(this + "Pre event "+newValue);
         return null; // filter out pre events
      }

      if (newValue!=null) {
         Call call = (Call) newValue;
         assert call.getListenerID() != null;
         if (!call.getListenerID().equals(listenerID)
               && !call.getListenerID().equals(ObjectFilterConverter.TOPOLOGY_CHANGE_UUID)) {
            if (log.isTraceEnabled()) log.trace(this + "Wrong listener "+call);
            return null;
         }
      }

      if (log.isTraceEnabled()) log.trace(this+" Passed "+newValue);

      return true;
   }

}
