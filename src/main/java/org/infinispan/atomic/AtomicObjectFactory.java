package org.infinispan.atomic;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.InvalidCacheUsageException;
import org.infinispan.atomic.container.AbstractContainer;
import org.infinispan.atomic.container.FakeContainer;
import org.infinispan.atomic.container.local.LocalContainer;
import org.infinispan.atomic.container.remote.RemoteContainer;
import org.infinispan.atomic.object.Reference;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.infinispan.atomic.object.Reference.unreference;

/**
 * @author Pierre Sutra
 */
public class AtomicObjectFactory {

   // Class fields

   private static Log log = LogFactory.getLog(AtomicObjectFactory.class);
   private static AtomicObjectFactory singleton;
   private static Map<BasicCache,AtomicObjectFactory> factories = new HashMap<>();

   public static AtomicObjectFactory getSingleton(){
      assert singleton !=null;
      return singleton;
   }

   public synchronized static AtomicObjectFactory forCache(BasicCache cache){
      return AtomicObjectFactory.forCache(cache, MAX_CONTAINERS);
   }

   public synchronized static AtomicObjectFactory forCache(BasicCache cache, int maxContainers){
      String cacheName =
            (cache.getName().equals(BasicCacheContainer.DEFAULT_CACHE_NAME))
                  ? "" : cache.getName(); // unify remote and embedded

      if (!factories.containsKey(cache))
         factories.put(cache,new AtomicObjectFactory(cache, maxContainers));

      if (singleton ==null && cacheName.equals("")) {
         singleton = factories.get(cache);
         log.info("AOF singleton  is "+ singleton);
      }

      return factories.get(cache);
   }

   protected static final int MAX_CONTAINERS=Integer.MAX_VALUE;
   public static final Map<Class,List<String>> updateMethods;
   static{
      updateMethods = new HashMap<>();

      updateMethods.put(List.class, new ArrayList<String>());
      updateMethods.get(List.class).add("retrieve");
      updateMethods.get(List.class).add("addAll");

      updateMethods.put(Set.class, new ArrayList<String>());
      updateMethods.get(Set.class).add("retrieve");
      updateMethods.get(Set.class).add("addAll");

      updateMethods.put(Map.class, new ArrayList<String>());
      updateMethods.get(Map.class).add("put");
      updateMethods.get(Map.class).add("putAll");
   }

   // Object fields

   private BasicCache cache;
   private final ConcurrentMap<Reference,AbstractContainer> registeredContainers;
   private int maxSize;

   /**
    *
    * Return an AtomicObjectFactory built on top of cache <i>c</i>.
    *
    * @param c a cache,  it must be synchronous.and non-transactional
    */
   private AtomicObjectFactory(BasicCache<Object, Object> c) throws InvalidCacheUsageException{
      this(c, MAX_CONTAINERS);
   }

   /**
    *
    * Returns an object factory built on top of cache <i>c</i> with a bounded amount <i>m</i> of
    * containers in it. Upon the removal of a container, the object is stored persistently in the cache.
    *
    * @param c it must be synchronous.and non-transactional
    * @param m max amount of containers kept by this factory.
    * @throws InvalidCacheUsageException
    */
   private AtomicObjectFactory(BasicCache<Object, Object> c, int m) throws InvalidCacheUsageException{
      cache = c;
      maxSize = m;
      assertCacheConfiguration();
      registeredContainers= CacheBuilder.newBuilder()
            .maximumSize(MAX_CONTAINERS)
            .removalListener(new RemovalListener<Reference, AbstractContainer>() {
               @Override
               public void onRemoval(RemovalNotification<Reference, AbstractContainer> objectObjectRemovalNotification) {
                  try {
                     AbstractContainer container = objectObjectRemovalNotification.getValue();
                     if (!container.isClosed()) container.close();
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            })
            .build().asMap();
      log.info(this + " Created");
   }

   public static AtomicObjectFactory get(String server) {
      int port = Integer.valueOf(server.split(":")[1]);
      String host = server.split(":")[0];
      org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
            = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
      cb.tcpNoDelay(true)
            .addServer()
            .host(host)
            .port(port);
      RemoteCacheManager manager= new RemoteCacheManager(cb.build());
      return forCache(manager.getCache());
   }

   public <T> T getInstanceOf(Class clazz) throws InvalidCacheUsageException{
      return (T) getInstanceOf(clazz, null, false, false);
   }

   public <T> T getInstanceOf(Reference reference) throws InvalidCacheUsageException{
      return (T) getInstanceOf(reference.getClazz(), reference.getKey(), false, false);
   }

   public <T> T getInstanceOf(Class clazz, Object key) throws InvalidCacheUsageException{
      return (T) getInstanceOf(clazz, key, false, false);
   }

   /**
    *
    * Returns an object of class <i>clazz</i>.
    * The class of this object must be initially serializable, as well as all the parameters of its methods.
    * Furthermore, the class must be deterministic.
    *
    * The object is atomic if <i>withReadOptimization</i> equals false; otherwise it is sequentially consistent..
    * In more details, if <i>withReadOptimization</i>  is set, every call to the object is first executed locally on a copy of the object, and in case
    * the call does not modify the state of the object, the value returned is the result of this tentative execution.
    *
    * @param clazz a class object
    * @param withReadOptimization set the read optimization on/off.
    * @return an object of the class <i>clazz</i>
    * @throws InvalidCacheUsageException
    */
   public <T> T getInstanceOf(Class<T> clazz, boolean withReadOptimization)
         throws InvalidCacheUsageException{
      return getInstanceOf(clazz, null, withReadOptimization, false);
   }

   /**
    *
    * Returns an object of class <i>clazz</i>.
    * The class of this object must be initially serializable, as well as all the parameters of its methods.
    * Furthermore, the class must be deterministic.
    *
    * The object is atomic if <i>withReadOptimization</i> equals false; otherwise it is sequentially consistent..
    * In more details, if <i>withReadOptimization</i>  is set, every call to the object is executed locally on a copy of the object, and in case
    * the call does not modify the state of the object, the value returned is the result of this tentative execution.
    * If the method <i>equalsMethod</i>  is not null, it overrides the default <i>clazz.equals()</i> when testing that the state of the object and
    * its copy are identical.
    *
    * @param clazz a class object
    * @param withReadOptimization set the read optimization on/off.
    * @param forceNew force the creation of the object, even if it exists already in the cache
    * @return an object of the class <i>clazz</i>
    * @throws InvalidCacheUsageException
    */
   public <T> T getInstanceOf(Class<T> clazz, Object key, boolean withReadOptimization, boolean forceNew, Object... initArgs)
         throws InvalidCacheUsageException {

      if (Map.class.isAssignableFrom(clazz)) {
         assert key!= null; // can only occur for static distributed field
         return (T) new DistributedObjectsMap(this,key,cache);
      }

      if( !(Serializable.class.isAssignableFrom(clazz))){
         throw new InvalidCacheUsageException(clazz+" should be serializable.");
      }

      // FIXME full check that the class is a Java Bean
      try{
         clazz.getConstructor();
      } catch (NoSuchMethodException e) {
         throw new InvalidCacheUsageException(clazz+" does not have an empty constructor.");
      }

      Reference reference;
      AbstractContainer container=null;

      try{

         if (key!=null) {
            reference = new Reference(clazz,key);
            container = registeredContainers.get(reference);
         }

         if(container==null) {

            if (cache instanceof RemoteCache) {
               container = new RemoteContainer(cache, clazz, key, withReadOptimization, forceNew, initArgs);
            } else if (cache instanceof AdvancedCache) {
               container = new LocalContainer(cache, clazz, key, withReadOptimization, forceNew, initArgs);
            } else {
               log.error("Using fake container.");
               container = new FakeContainer(cache, clazz, key, withReadOptimization, forceNew, initArgs);
            }

            reference = container.getReference();
            if (registeredContainers.putIfAbsent(reference, container)==null) {
               if (log.isTraceEnabled())
                  log.trace(this + " adding " + container + " with " + container.getReference());
            }
            container = registeredContainers.get(reference);
         }

      } catch (Exception e){
         e.printStackTrace();
         throw new InvalidCacheUsageException(e.getCause());
      }

      assert container!=null;

      return (T) container.getProxy();

   }

   /**
    * TO BE REMOVED (might lead to inconsistencies)
    */
   @Deprecated
   public void disposeInstanceOf(Class clazz, Object key)
         throws InvalidCacheUsageException {

      Reference reference = new Reference<>(clazz,key);

      AbstractContainer container = registeredContainers.get(reference);

      if (log.isDebugEnabled())
         log.debug(this + " Dispsing " + container);

      if( container == null ) return;

      try{
         container.close();
      }catch (Exception e){
         e.printStackTrace();
         throw new InvalidCacheUsageException("Error while disposing object "+key);
      }

      registeredContainers.remove(reference);

   }

   public void close(){
      for (AbstractContainer container : registeredContainers.values())
         try {
            container.close();
         } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            e.printStackTrace();
         }
      log.info(this+"Closed");
   }

   @Override
   public String toString(){
      return "AOF["+cache.toString()+"]";
   }

   // Helpers

   public void assertCacheConfiguration() throws InvalidCacheUsageException {
      if (cache instanceof Cache
            &&
            (
                  ((Cache)cache).getCacheConfiguration().transaction().transactionMode().isTransactional()
                        ||
                        ((Cache)cache).getCacheConfiguration().locking().useLockStriping()
            ))
         throw new InvalidCacheUsageException("Cache should not be transactional, nor use lock stripping."); // as of 7.2.x
   }


   public static class DistributedObjectsMap implements Map{

      private AtomicObjectFactory factory;
      private Object key;
      private BasicCache cache;

      public DistributedObjectsMap(AtomicObjectFactory factory, Object key, BasicCache cache) {
         this.factory = factory;
         this.key= key;
         this.cache = cache;
      }

      private Object transformKey(Object key) {
         return key.toString()+"#"+ key.toString(); // FIXME
      }

      @Override
      public Object get(Object key) {
         Object object = cache.get(transformKey(key));
         if (object instanceof Reference)
            return unreference((Reference)object,factory);
         return object;
      }


      @Override
      public int size() {
         return cache.size();
      }

      @Override
      public boolean isEmpty() {
         return cache.isEmpty();
      }

      @Override
      public boolean containsKey(Object key) {
         return cache.containsKey(transformKey(key));
      }

      @Override
      public boolean containsValue(Object value) {
         return cache.containsValue(value);
      }

      @Override
      public Object put(Object key, Object value) {
         return cache.put(transformKey(key), value);
      }

      @Override
      public Object remove(Object key) {
         return cache.remove(transformKey(key));
      }

      @Override
      public void putAll(Map m) {
         Set<Map.Entry> entries = m.entrySet();
         for(Map.Entry entry : entries) {
            put(transformKey(entry.getKey()),entry.getValue());
         }
      }

      @Override
      public void clear() {
         cache.clear();
      }

      @Override
      public Set keySet() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Collection values() {
         return cache.values();
      }

      @Override
      public Set<Entry> entrySet() {
         throw new UnsupportedOperationException();
      }

      @Override
      public String toString(){
         return cache.toString();
      }

   }

}
