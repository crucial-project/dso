package org.infinispan.atomic.filter;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.object.*;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheAware;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.infinispan.atomic.object.Reference.unreference;
import static org.infinispan.atomic.object.Utils.marshall;
import static org.infinispan.atomic.object.Utils.unmarshall;

/**
 * @author Pierre Sutra
 */
public class ObjectFilterConverter extends AbstractCacheEventFilterConverter<Reference,Call,CallFuture>
      implements CacheAware<Reference,Call>, Externalizable{

   // Class fields & methods
   private static Log log = LogFactory.getLog(ObjectFilterConverter.class);

   // Object fields
   private Cache<Reference,Call> cache;
   private ConcurrentMap<Reference,Object> objects;
   private ConcurrentMap<Reference,CallClose> pendingCloseCalls;
   private ConcurrentMap<Reference,Boolean> includeState;
   private ConcurrentHashMap<Object,AtomicInteger> openCallsCounters;

   public ObjectFilterConverter(){
      this.openCallsCounters = new ConcurrentHashMap<>();
      this.objects = new ConcurrentHashMap<>();
      this.pendingCloseCalls = new ConcurrentHashMap<>();
      this.includeState = new ConcurrentHashMap<>();
   }

   @Override
   public void setCache(Cache <Reference,Call> cache) {
      this.cache = cache;
      AtomicObjectFactory.forCache(cache);
   }

   @Override
   public CallFuture filterAndConvert(Reference reference, Call oldValue, Metadata oldMetadata, Call newValue,
         Metadata newMetadata, EventType eventType) {

      assert (cache!=null);

      if (!openCallsCounters.containsKey(reference)) {
         openCallsCounters.putIfAbsent(reference, new AtomicInteger(0));
         includeState.put(reference,Utils.hasReadOnlyMethods(reference.getClazz()));
      }

      synchronized (openCallsCounters.get(reference)) {

         // when receiving a null value and there is no pending close call,
         // we clear everything from that reference
         if (newValue == null) {
            if (!pendingCloseCalls.containsKey(reference)) {
               objects.remove(reference);
               pendingCloseCalls.remove(reference);
               openCallsCounters.remove(reference);
               includeState.remove((reference));
            }
            return null;
         }

         try {

            Call call = newValue;

            if (log.isTraceEnabled())
               log.trace(this + "- Received [" + call + "]");

            CallFuture future = new CallFuture(call.getCallID());

            if (call instanceof CallInvoke) {

               CallInvoke invocation = (CallInvoke) call;

               Object[] args = unreference(
                     invocation.arguments,
                     cache);

               Object responseValue =
                     Utils.callObject(
                           objects.get(reference),
                           invocation.method,
                           args);

               future.set(responseValue);
               if (includeState.get(reference))
                  future.setState(objects.get(reference));

               if (log.isTraceEnabled())
                  log.trace(this + "- Called " + invocation + " (=" + (responseValue == null ?
                        "null" :
                        responseValue.toString()) + ")");

            } else if (call instanceof CallPersist) {

               if (log.isTraceEnabled())
                  log.trace(this + "- Retrieved CallPersist [" + reference + "]");

               assert (pendingCloseCalls.get(reference) != null);
               future = new CallFuture(pendingCloseCalls.get(reference).getCallID());
               future.set(null);
               pendingCloseCalls.remove(reference);
               if (openCallsCounters.get(reference).get() == 0) {
                  pendingCloseCalls.remove(reference);
                  openCallsCounters.remove(reference);
                  objects.remove(reference);
               }

            } else if (call instanceof CallOpen) {

               CallOpen callOpen = (CallOpen) call;

               openCallsCounters.get(reference).incrementAndGet();

               if (callOpen.getForceNew()) {

                  if (log.isTraceEnabled())
                     log.trace(this + "- Forcing new object [" + reference + "]");

                  objects.put(
                        reference,
                        Utils.initObject(
                              reference.getClazz(),
                              unreference(
                                    callOpen.getInitArgs(),
                                    cache)));

               } else if (objects.get(reference) == null) {

                  if (oldValue == null) {

                     if (log.isTraceEnabled())
                        log.trace(this + "- Creating new object [" + reference + "]");
                     objects.put(
                           reference,
                           Utils.initObject(
                                 reference.getClazz(),
                                 unreference(
                                       callOpen.getInitArgs(),
                                       cache)));

                  } else {

                     if (oldValue instanceof CallPersist) {

                        if (log.isTraceEnabled())
                           log.trace(this + "- Retrieving object from persistent state [" + reference + "]");
                        objects.put(reference, unmarshall(((CallPersist) oldValue).getBytes()));

                     } else {

                        throw new IllegalStateException("Cannot rebuild object [" + reference + "]; having: "+oldValue.getClass());

                     }

                  }

               }

               future.set(null);

            } else if (call instanceof CallClose) {

               if (openCallsCounters.get(reference).get() > 0) openCallsCounters.get(reference).decrementAndGet();

               if (openCallsCounters.get(reference).get() == 0 && pendingCloseCalls.get(reference) == null) {

                  assert (objects.get(reference) != null);
                  if (log.isTraceEnabled())
                     log.trace(this + "- Persisting object [" + reference + "," + isLocalPrimaryOwner(reference) + "]");

                  pendingCloseCalls.put(reference, (CallClose) call);
                  if (isLocalPrimaryOwner(reference))
                     cache.putAsync(
                           reference,
                           new CallPersist(call.getListenerID(), marshall(objects.get(reference))));

               } else {

                  future.set(null);

               }

            }

            if (log.isTraceEnabled())
               log.trace(this + "- Future (" + future.getCallID() + ", " + reference + ") -> " + future.isDone());

            if (future.isDone())
               return future;

         } catch (Exception e) {
            e.printStackTrace();
         }

      }

      return null;

   }

   @Override
   public String toString(){
      return "ObjectFilterConverter";
   }

   @Override
   public synchronized void writeExternal(ObjectOutput objectOutput) throws IOException {
      // nothing to do
   }

   @Override
   public synchronized void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      // nothing to do
   }

   // Helpers

   private boolean isLocalPrimaryOwner(Reference reference) {
      return cache.getAdvancedCache().getDistributionManager().getConsistentHash()
            .locatePrimaryOwner(reference).equals(
                  cache.getAdvancedCache().getRpcManager().getAddress());
   }

}
