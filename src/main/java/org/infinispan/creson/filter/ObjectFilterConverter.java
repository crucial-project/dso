package org.infinispan.creson.filter;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.google.common.cache.CacheBuilder;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.creson.Factory;
import org.infinispan.creson.object.*;
import org.infinispan.creson.utils.ThreadLocalUUIDGenerator;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheAware;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pierre Sutra
 */
public class ObjectFilterConverter extends AbstractCacheEventFilterConverter<Reference,Call,CallFuture>
      implements CacheAware<Reference,Call>, Externalizable {

   // Class fields & methods
   private static final Log log = LogFactory.getLog(ObjectFilterConverter.class);
   private static final long MAX_COMPLETED_CALLS = 1000; // around 1s at max throughput, not much... and already too much
   public static final UUID TOPOLOGY_CHANGE_UUID = new UUID(0, 0);

   // Per-object fields
   private Cache<Reference, Call> cache;
   private final ConcurrentMap<Reference, Object> objects;
   private final ConcurrentMap<Reference, AtomicInteger> openCallsCounters;
   private final ConcurrentMap<Reference, Boolean> includeStateTrackers;
   private final ConcurrentMap<Reference, RandomBasedGenerator> generators;
   private final ConcurrentMap<Reference, AtomicBoolean> topologyChanging;

   // Other fields
   private final ConcurrentMap<Reference,Map<Call, CallFuture>> completedCalls;
   private TopologyChangeListener topologyChangeListener;

   public ObjectFilterConverter() {
      this.objects = new ConcurrentHashMap<>();
      this.includeStateTrackers = new ConcurrentHashMap<>();
      this.generators = new ConcurrentHashMap<>();

      this.openCallsCounters = new ConcurrentHashMap<>();
      this.topologyChanging = new ConcurrentHashMap<>();
      this.completedCalls = new ConcurrentHashMap<>();
   }

   @Override
   public void setCache(Cache<Reference, Call> cache) {
      assert this.cache == null;
      assert this.topologyChangeListener == null;

      if (log.isTraceEnabled())log.trace(" setCache(" + cache + ")");
      this.cache = cache;
      Factory.forCache(cache,
            (int) cache.getAdvancedCache().getCacheConfiguration().eviction().maxEntries()/2);

      this.topologyChangeListener = new TopologyChangeListener();
      this.cache.addListener(topologyChangeListener);
   }

   @Override
   public CallFuture filterAndConvert(Reference reference, Call oldValue, Metadata oldMetadata, Call newValue,
         Metadata newMetadata, EventType eventType) {

      assert (cache != null);

      if (log.isTraceEnabled())
         log.trace(" Accessing " + reference);

      if (!openCallsCounters.containsKey(reference)) {
         openCallsCounters.putIfAbsent(reference, new AtomicInteger(0));
         includeStateTrackers.putIfAbsent(reference, Utils.hasReadOnlyMethods(reference.getClazz()));
         topologyChanging.put(reference, new AtomicBoolean(false));
         completedCalls.put(reference,
               (ConcurrentMap) CacheBuilder.newBuilder()
                     .maximumSize(MAX_COMPLETED_CALLS)
                     .build().asMap());
      }

      synchronized (openCallsCounters.get(reference)) {

         Call call = newValue;

          assert !(call instanceof CallPersist);

          if (log.isTraceEnabled())
             log.trace(" Received [" + call + "] (completed="
                   + completedCalls.containsKey(call) + ", " + reference + " ," + eventType.getType() + ")");

         CallFuture future = new CallFuture(call.getCallID());

         if (completedCalls.get(reference).containsKey(call)) {

            // call already completed
            future = completedCalls.get(reference).get(call);

         } else {

            try {

               assert !objects.containsKey(reference) || generators.containsKey(reference);

               if ( topologyChanging.get(reference).get()
                     && ((call instanceof CallPersist) || (oldValue instanceof CallPersist)) )
                  topologyChanging.get(reference).set(false);

               if (!keepCall(call, reference, oldValue)) {
                  if (log.isDebugEnabled())
                     log.debug("Trashing " + call + "; " + oldValue + "; " + eventType.getType());
                  return null;

               } else if (call instanceof CallInvoke) {

                  CallInvoke invocation = (CallInvoke) call;

                  assert objects.containsKey(reference);

                  Object[] args = Reference.unreference(
                        invocation.arguments,
                        cache);

                  ThreadLocalUUIDGenerator.setThreadLocal(generators.get(reference));

                  try {
                     Object response =
                           Utils.callObject(
                                 objects.get(reference),
                                 invocation.method,
                                 args);

                     future.set(response);

                     if (includeStateTrackers.get(reference))
                        future.setState(objects.get(reference));

                     if (log.isTraceEnabled())
                        log.trace(" Called " + invocation + " on "+reference+" (=" +
                              (response == null ? "null" : response.toString()) + ")");

                  }catch (Throwable e){
                     future.set(e);
                  }

                  ThreadLocalUUIDGenerator.unsetThreadLocal();

               } else if (call instanceof CallOpen) {

                  CallOpen callOpen = (CallOpen) call;

                  assert !objects.containsKey(reference) || objects.get(reference) != null;

                  if (!generators.containsKey(reference))
                     generators.putIfAbsent(
                           reference,
                           new RandomBasedGenerator(
                                 new Random(call.getCallID().getLeastSignificantBits()
                                       +call.getCallID().getMostSignificantBits())));

                  if (callOpen.getForceNew()) {

                     if (log.isTraceEnabled())
                        log.trace(" New (forced) [" + reference + "]");

                     createObejct(reference, callOpen);

                  } else if (!objects.containsKey(reference)) {

                     if (oldValue == null) {

                        if (log.isTraceEnabled())
                           log.trace(" New [" + reference + "]");

                        createObejct(reference, callOpen);

                     } else {

                        if (oldValue instanceof CallPersist)  {

                           retrieveReference(reference, (CallPersist) oldValue);

                        } else {

                           throw new IllegalStateException(
                                 "Cannot rebuild [" + reference + "]; having: " + oldValue.getClass());

                        }

                     }

                  }

                  assert generators.containsKey(reference) && objects.containsKey(reference);

                  openCallsCounters.get(reference).incrementAndGet();

                  if (log.isTraceEnabled())
                        log.trace(" OpenCallsCounters = "+openCallsCounters.get(reference));

                  future.set(null);

               } else if (call instanceof CallClose) {

                  assert openCallsCounters.get(reference).get() > 0;

                  openCallsCounters.get(reference).decrementAndGet();

                  if (log.isTraceEnabled())
                     log.trace(" OpenCallsCounters = " + openCallsCounters.get(reference));

                  if (openCallsCounters.get(reference).get() == 0) {
                     persistReference(reference);
                     cleanUpReference(reference);
                  }

                  future.set(null);

               }

               // save return value if any
               if (future.isDone())
                  completedCalls.get(reference).put(call, future);

            } catch (Exception e) {
               e.printStackTrace();
            }

         } // end compute return value

         assert !future.isCancelled();
         assert future.isDone();

         if (log.isTraceEnabled())
            log.trace(" Future (" + future.getCallID() + ", " + reference + ") -> "
                  + (future.isDone() && isLocalPrimaryOwner(reference)));

         if (future.isDone()) {
            return isLocalPrimaryOwner(reference) ? future : null;
         }

      } // synchronized(reference)

      return null;

   }

   private boolean keepCall(Call call, Reference reference, Call oldValue) {

      if (topologyChanging.get(reference).get())
         return false;

      if (call instanceof CallPersist)
         return true;

      if (objects.containsKey(reference)) {
         return true;
      } else {
         if (call instanceof CallOpen) {
            return ((CallOpen) call).getForceNew() || (oldValue == null) || (oldValue instanceof CallPersist);
         }
      }

      return false;

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

   private boolean isLocalOwner(Reference reference, ConsistentHash consistentHash) {
      return consistentHash.locateOwners(reference).contains(
            cache.getAdvancedCache().getRpcManager().getAddress());
   }

   private boolean isLocalPrimaryOwner(Reference reference) {
      return isLocalPrimaryOwner(reference, cache.getAdvancedCache().getDistributionManager().getConsistentHash());
   }

   private boolean isLocalPrimaryOwner(Reference reference, ConsistentHash consistentHash) {
      return consistentHash.locatePrimaryOwner(reference).equals(
            cache.getAdvancedCache().getRpcManager().getAddress());
   }

   private boolean isOwnershipChanged(Reference reference, ConsistentHash start, ConsistentHash end) {
      Set<Address> membershipStart = new HashSet<>(start.locateOwners(reference));
      Set<Address> membershipEnd = new HashSet<>(end.locateOwners(reference));
      return ! (membershipStart.containsAll(membershipEnd) && membershipEnd.containsAll(membershipStart));
   }

   private boolean isForwarding(Reference reference, ConsistentHash start, ConsistentHash end) {

      if (isLocalPrimaryOwner(reference, start))
         if (end.getMembers().contains(cache.getAdvancedCache().getRpcManager().getAddress()))
            return true;
         else
            return false;

      if (end.getMembers().contains(start.locatePrimaryOwner(reference)))
         return false;

      Address candidate = null;
      for(Address member : start.locateOwners(reference)) {
         if (!member.equals(start.locatePrimaryOwner(reference))) {
            candidate = member;
            break;
         }
      }
      assert candidate != null;
      return candidate.equals(cache.getAdvancedCache().getRpcManager().getAddress());
   }

   private boolean isLocalNodeNewComer(ConsistentHash consistentHash){
      return !consistentHash.getMembers().contains(
            cache.getAdvancedCache().getRpcManager().getAddress());
   }

   private boolean isStable(ConsistentHash start, ConsistentHash end) {
      if (start.getMembers().size()!=end.getMembers().size()) return false;
      Collection<Address> newcomers
            = new ArrayList<>(end.getMembers());
      newcomers.removeAll(start.getMembers());
      for (Address address : newcomers) {
         if (end.getSegmentsForOwner(address).size()==0)
            return false;
      }
      return true;
   }

   private boolean isStable(ConsistentHash consistentHash) {
      for (Address address : consistentHash.getMembers()) {
         if (consistentHash.getSegmentsForOwner(address).size()==0)
            return false;
      }
      return true;
   }

   private void cleanUpReference(Reference reference) {
      if (log.isTraceEnabled())
         log.trace(" Cleaning-up [" + reference + "]");
      objects.remove(reference);
      includeStateTrackers.remove((reference));
      generators.remove(reference);
      openCallsCounters.remove(reference);
   }

   private void persistReference(Reference reference) {

      assert (objects.get(reference) != null);

      if (log.isTraceEnabled())
         log.trace(" Persisting [" + reference + "]");

      byte[] marshalledObject = Utils.marshall(objects.get(reference));

      int openCallsCounter = openCallsCounters.get(reference).get();

      cache.getAdvancedCache()
            .withFlags(
                  Flag.CACHE_MODE_LOCAL,
                  Flag.SKIP_LISTENER_NOTIFICATION,
                  Flag.IGNORE_RETURN_VALUES,
                  Flag.SKIP_CACHE_LOAD)
            .putAsync(reference,
                  new CallPersist(
                        TOPOLOGY_CHANGE_UUID,
                        generators.get(reference).generate(),
                        marshalledObject,
                        openCallsCounter));
   }

//   private void forwardeference(Reference reference) {
//
//      assert (objects.get(reference) != null);
//
//      if (log.isTraceEnabled())
//         log.trace(" Forwarding [" + reference + "]");
//
//      byte[] marshalledObject = marshall(objects.get(reference));
//
//      int openCallsCounter = openCallsCounters.get(reference).get();
//
//      cache.getAdvancedCache()
//            .putAsync(reference,
//                  new CallPersist(
//                        TOPOLOGY_CHANGE_UUID,
//                        generators.get(reference).generate(),
//                        marshalledObject,
//                        openCallsCounter));
//   }

   private void retrieveReference(Reference reference, CallPersist oldValue) {

      objects.put(reference, Utils.unmarshall(oldValue.getBytes()));

      generators.put(
            reference,
            new RandomBasedGenerator(
                  new Random(oldValue.getCallID().getLeastSignificantBits()))); // type 4 UUID

      openCallsCounters.get(reference).set(oldValue.getOpenCallsCounter());

      if (log.isTraceEnabled())
         log.trace(" Retrieving [" + reference + "]");

      assert objects.get(reference) != null;

   }

   private void createObejct(Reference reference, CallOpen callOpen)
         throws IllegalAccessException, InstantiationException,
         NoSuchMethodException, InvocationTargetException, NoSuchFieldException {

      Object ret = Utils.initObject(reference.getClazz(), Reference.unreference(callOpen.getInitArgs(), cache));

      // force the key field, in case it is created per default
      if (reference.getClazz().getAnnotation(Entity.class)!=null) {
         Field field = null;
         for (Field f : reference.getClazz().getFields()) {
            if (f.getAnnotation(Id.class) != null) {
               field = f;
               break;
            }
         }
         field.set(ret, reference.getKey());
      }

      objects.put(
            reference,
            ret);

   }

   @Listener
   private class TopologyChangeListener {

//      @DataRehashed
//      public void topologyChangeIN(DataRehashedEvent event) {
//
//         if (!event.isPre())
//            return;
//
//         ConsistentHash startCH = event.getConsistentHashAtStart();
//         ConsistentHash endCH = event.getConsistentHashAtEnd();
//
//         if (log.isTraceEnabled())
//            log.trace("Topology " + event.getNewTopologyId()+" is installing");
//
//         for (Map.Entry<Reference, AtomicInteger> entry : openCallsCounters.entrySet()) {
//
//            Reference reference = entry.getKey();
//
//            if (!isOwnershipChanged(reference, startCH, endCH))
//               continue;
//
//            topologyChanging.get(reference).set(true);
//
//         }
//
//      }

      @DataRehashed
      public void topologyChangeOUT(DataRehashedEvent event) {

         if (!event.isPre())
            return;

         ConsistentHash startCH = event.getConsistentHashAtStart();
         ConsistentHash endCH = event.getConsistentHashAtEnd();

         if (isLocalNodeNewComer(startCH))
            return;

         log.info("Topology changed");

         for (Map.Entry<Reference, AtomicInteger> entry : openCallsCounters.entrySet()) {

            Reference reference = entry.getKey();

            AtomicInteger openCallCounter = entry.getValue();

            if (!isOwnershipChanged(reference, startCH, endCH))
               continue;

            synchronized (openCallCounter) {
               if (objects.containsKey(reference) && openCallCounter.get()!=0) {
                  assert generators.containsKey(reference) && objects.containsKey(reference);
                  persistReference(reference);
               }
            }

         }

         log.info("Topology " + event.getNewTopologyId()+" installed");

      }

      @CacheEntryPassivated
      public void passivationTrigger(CacheEntryPassivatedEvent event) {
         if (event.getKey() instanceof Reference) {
            Reference reference = (Reference) event.getKey();
            if (log.isTraceEnabled())
               log.trace(reference + " passivated [" + (event.getKey() instanceof CallPersist) + "]");
         }
      }

   }

}
