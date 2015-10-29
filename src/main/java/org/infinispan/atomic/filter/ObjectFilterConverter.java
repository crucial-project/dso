package org.infinispan.atomic.filter;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.google.common.cache.CacheBuilder;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.object.*;
import org.infinispan.atomic.utils.UUIDGenerator;
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
import java.util.Random;
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
   private static final Log log = LogFactory.getLog(ObjectFilterConverter.class);
   private static final long MAX_COMPLETED_CALLS = 10000; // around 10s at max throughput

   // Per-object fields
   private Cache<Reference,Call> cache;
   private final ConcurrentMap<Reference,Object> objects;
   private final ConcurrentMap<Reference,CallClose> pendingCloseCalls;
   private final ConcurrentMap<Reference,Boolean> includeStateTrackers;
   private final ConcurrentMap<Reference, RandomBasedGenerator> generators;

   // Other fields
   private final ConcurrentMap<Object,AtomicInteger> openCallsCounters;
   private final ConcurrentMap<Call, CallFuture> completedCalls;

   public ObjectFilterConverter(){
      this.objects = new ConcurrentHashMap<>();
      this.pendingCloseCalls = new ConcurrentHashMap<>();
      this.includeStateTrackers = new ConcurrentHashMap<>();
      this.generators = new ConcurrentHashMap<>();

      this.openCallsCounters = new ConcurrentHashMap<>();
      this.completedCalls = (ConcurrentMap) CacheBuilder.newBuilder()
            .maximumSize(MAX_COMPLETED_CALLS)
            .build().asMap();
   }

   @Override
   public void setCache(Cache <Reference,Call> cache) {
      if (log.isTraceEnabled()) log.trace(" setCache("+cache+")");
      this.cache = cache;
      AtomicObjectFactory.forCache(cache);
   }

   @Override
   public CallFuture filterAndConvert(Reference reference, Call oldValue, Metadata oldMetadata, Call newValue,
         Metadata newMetadata, EventType eventType) {

      assert (cache!=null);

      // FIXME handle topology changes

      if (log.isTraceEnabled()) log.trace(" Accessing "+reference);

      if (!openCallsCounters.containsKey(reference)) {
         openCallsCounters.putIfAbsent(reference,new AtomicInteger(0));
         includeStateTrackers.putIfAbsent(reference,Utils.hasReadOnlyMethods(reference.getClazz()));
      }

      synchronized (openCallsCounters.get(reference)) {

         // when receiving a null value and there is no pending close call,
         // we clear everything from that reference
         if (newValue == null) {
            if (!pendingCloseCalls.containsKey(reference)) {
               objects.remove(reference);
               includeStateTrackers.remove((reference));
               generators.remove(reference);
               pendingCloseCalls.remove(reference);
               openCallsCounters.remove(reference);
            }
            return null;
         }

         Call call = newValue;

         if (log.isTraceEnabled())
            log.trace(" Received [" + call + "] (completed=" + completedCalls.containsKey(call)+")");

         CallFuture future = new CallFuture(call.getCallID());

         if (call.equals(pendingCloseCalls.get(reference))) {

            // ignore, close call received twice

         } else if (completedCalls.containsKey(call)) {

            // call already completed
            future = completedCalls.get(call);

         } else {

            try {

               if (call instanceof CallInvoke) {

                  CallInvoke invocation = (CallInvoke) call;

                  assert objects.containsKey(reference);

                  Object[] args = unreference(
                        invocation.arguments,
                        cache);

                  UUIDGenerator.setThreadLocal(generators.get(reference));

                  Object responseValue =
                        Utils.callObject(
                              objects.get(reference),
                              invocation.method,
                              args);

                  UUIDGenerator.unsetThreadLocal();

                  future.set(responseValue);
                  if (includeStateTrackers.get(reference))
                     future.setState(objects.get(reference));

                  if (log.isTraceEnabled())
                     log.trace(" Called " + invocation + " (=" + (responseValue == null ?
                           "null" :
                           responseValue.toString()) + ")");

               } else if (call instanceof CallPersist) {

                  if (log.isTraceEnabled())
                     log.trace(" Retrieved CallPersist [" + reference + "]");

                  assert (pendingCloseCalls.get(reference) != null);
                  future = new CallFuture(pendingCloseCalls.get(reference).getCallID());
                  future.set(null);
                  pendingCloseCalls.remove(reference);
                  if (openCallsCounters.get(reference).get() == 0) {
                     includeStateTrackers.remove(reference);
                     generators.remove(reference);
                     objects.remove(reference);
                     openCallsCounters.remove(reference);
                  }

               } else if (call instanceof CallOpen) {

                  CallOpen callOpen = (CallOpen) call;

                  openCallsCounters.get(reference).incrementAndGet();

                  assert !objects.containsKey(reference) || objects.get(reference)!=null;

                  if (!generators.containsKey(reference))
                     generators.putIfAbsent(
                           reference,
                           Generators.randomBasedGenerator(
                                 new Random(
                                       callOpen.getCallID().getLeastSignificantBits()))); // type 4 UUID

                  if (callOpen.getForceNew()) {

                     if (log.isTraceEnabled())
                        log.trace(" Forcing new object [" + reference + "]");

                     objects.put(
                           reference,
                           Utils.initObject(
                                 reference.getClazz(),
                                 unreference(
                                       callOpen.getInitArgs(),
                                       cache)));

                  } else if (!objects.containsKey(reference)) {

                     if (oldValue == null) {

                        if (log.isTraceEnabled())
                           log.trace(" Creating new object [" + reference + "]");

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
                              log.trace(" Retrieving object from persistent state [" + reference + "]");
                           objects.put(reference, unmarshall(((CallPersist) oldValue).getBytes()));

                        } else {

                           throw new IllegalStateException(
                                 "Cannot rebuild object [" + reference + "]; having: " + oldValue.getClass());

                        }

                     }

                  }

                  future.set(null);

               } else if (call instanceof CallClose) {

                  assert openCallsCounters.get(reference).get()>=0;

                  if (openCallsCounters.get(reference).get() > 0)
                     openCallsCounters.get(reference).decrementAndGet();

                  if (openCallsCounters.get(reference).get() == 0 && pendingCloseCalls.get(reference) == null) {

                     assert (objects.get(reference) != null);
                     if (log.isTraceEnabled())
                        log.trace(
                              " Persisting object [" + reference + "," + isLocalPrimaryOwner(reference) + "]");

                     pendingCloseCalls.put(reference, (CallClose) call);
                     if (isLocalPrimaryOwner(reference))
                        cache.putAsync(
                              reference,
                              new CallPersist(call.getListenerID(), UUIDGenerator.generate(), marshall(objects.get(reference))));

                  } else {

                     future.set(null);

                  }

               }

               // save return value if any
               if (future.isDone())
                  completedCalls.put(call,future);

            } catch (Exception e) {
               e.printStackTrace();
            }

         } // end compute return value

         assert !future.isCancelled();
         assert future.isDone() || (call instanceof CallClose);

         if (log.isTraceEnabled())
            log.trace(" Future (" + future.getCallID() + ", " + reference + ") -> " + future.isDone());

         if (future.isDone())
            return isLocalPrimaryOwner(reference) ? future : null;

      } // synchronized(reference)

      return null;

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
