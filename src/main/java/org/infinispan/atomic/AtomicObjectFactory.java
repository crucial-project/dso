package org.infinispan.atomic;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.infinispan.Cache;
import org.infinispan.InvalidCacheUsageException;
import org.infinispan.atomic.container.AbstractContainer;
import org.infinispan.atomic.object.Reference;
import org.infinispan.atomic.container.local.LocalContainer;
import org.infinispan.atomic.container.remote.RemoteContainer;
import org.infinispan.atomic.object.Utils;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class AtomicObjectFactory {

   // Class fields
   
   private static Log log = LogFactory.getLog(AtomicObjectFactory.class);
   private static Map<String,AtomicObjectFactory> factories = new HashMap<>();
   public synchronized static AtomicObjectFactory forCache(String cacheName){
      return factories.get(cacheName);
   }
   public synchronized static AtomicObjectFactory forCache(BasicCache cache){
      String cacheName = 
            (cache.getName().equals(BasicCacheContainer.DEFAULT_CACHE_NAME))
                  ? "" : cache.getName(); // unify remote and embedded 
      if(!factories.containsKey(cacheName))
         factories.put(
               cacheName,
               new AtomicObjectFactory(cache));
      return factories.get(cacheName);
   }
   
   protected static final int MAX_CONTAINERS=1000;// 0 means no limit
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
   public AtomicObjectFactory(BasicCache<Object, Object> c) throws InvalidCacheUsageException{
      this(c,MAX_CONTAINERS);
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
   public AtomicObjectFactory(BasicCache<Object, Object> c, int m) throws InvalidCacheUsageException{
      cache = c;
      maxSize = m;
      assertCacheConfiguration();
      registeredContainers= CacheBuilder.newBuilder()
            .maximumSize(MAX_CONTAINERS)
            .removalListener(new RemovalListener<Reference, AbstractContainer>() {
               @Override 
               public void onRemoval(RemovalNotification<Reference, AbstractContainer> objectObjectRemovalNotification) {
                  try {
                     objectObjectRemovalNotification.getValue().close();
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            })
            .build().asMap();
      log.info(this+"Created");
   }

   public <T> T getInstanceOf(Reference reference)
         throws InvalidCacheUsageException{
      return (T) getInstanceOf(reference, false, null, false);
   } 
   
   /**
    *
    * Returns an atomic object of class <i>clazz</i>.
    * The class of this object must be initially serializable, as well as all the parameters of its methods.
    * Furthermore, the class must be deterministic.
    * 
    * This method is an alias for getInstanceOf(clazz, key, false, null, false).  
    *
    * @param clazz a class object
    * @param key to use in order to store the object.
    * @return an object of the class <i>clazz</i>
    * @throws InvalidCacheUsageException
    */
   public <T> T getInstanceOf(Class<T> clazz, Object key)
         throws InvalidCacheUsageException{
      return getInstanceOf(clazz, key, false, null, false);
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
    * @param key the key to use in order to store the object.
    * @param withReadOptimization set the read optimization on/off.
    * @return an object of the class <i>clazz</i>
    * @throws InvalidCacheUsageException
    */
   public <T> T getInstanceOf(Class<T> clazz, Object key, boolean withReadOptimization)
         throws InvalidCacheUsageException{
      return getInstanceOf(clazz, key, withReadOptimization, null, false);
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
    * @param key the key to use in order to store the object.
    * @param withReadOptimization set the read optimization on/off.
    * @param equalsMethod overriding the default <i>clazz.equals()</i>.
    * @param forceNew force the creation of the object, even if it exists already in the cache
    * @return an object of the class <i>clazz</i>
    * @throws InvalidCacheUsageException
    */
   public <T> T getInstanceOf(Class<T> clazz, Object key, boolean withReadOptimization, Method equalsMethod, boolean forceNew, Object ... initArgs)
         throws InvalidCacheUsageException {
      Reference<T> reference = new Reference(clazz,key);
      return getInstanceOf(reference,withReadOptimization,equalsMethod,forceNew,initArgs);
   }


   public synchronized  <T> T getInstanceOf(Reference<T> reference, boolean withReadOptimization, Method equalsMethod, boolean forceNew, Object ... initArgs)
         throws InvalidCacheUsageException {

      if (Utils.isDistributed(reference.getClazz()) 
            && !Utils.hasDefaultConstructor(reference.getClazz()))
         throw new InvalidCacheUsageException("Should have a default constructor.");
      
      if( !(Serializable.class.isAssignableFrom(reference.getClazz()))){
         throw new InvalidCacheUsageException("Should be serializable.");
      }
      
      AbstractContainer container;

      try{

         container = registeredContainers.get(reference);

         if( container==null){

            if (log.isDebugEnabled()) log.debug(this + " Creating container");

            container =
                  (cache instanceof RemoteCache) ?
                        new RemoteContainer(cache, reference, withReadOptimization, forceNew, initArgs)
                        :
                        new LocalContainer(cache, reference, withReadOptimization, forceNew, initArgs);
            
            registeredContainers.putIfAbsent(reference, container);

         } else {
            if (log.isDebugEnabled()) log.debug(this + " Existing container");
         }

      } catch (Exception e){
         e.printStackTrace();
         throw new InvalidCacheUsageException(e.getCause());
      }

      return (T) container.getProxy();

   }

   /**
    * Remove the object stored at <i>key</i>from the local state.
    * If flag <i>keepPersistent</i> is set, a persistent copy of the current state of the object is also stored in the cache.
    *
    * @param clazz a class object
    * @param key the key to use in order to store the object.
    * @param keepPersistent indicates that a persistent copy is stored in the cache or not.
    */
   @Deprecated
   public void disposeInstanceOf(Class clazz, Object key, boolean keepPersistent)
         throws InvalidCacheUsageException {

      Reference reference = new Reference<>(clazz,key);
      AbstractContainer container;
      synchronized (registeredContainers){
         container = registeredContainers.get(reference);
         if( container == null ) return;
         registeredContainers.remove(reference);
      }

      try{
         container.close();
      }catch (Exception e){
         e.printStackTrace();
         throw new InvalidCacheUsageException("Error while disposing object "+key);
      }

   }
   
   public void close(){
      log.info(this+"Closing");
      for (AbstractContainer container : registeredContainers.values())
         try {
            container.close();
         } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            e.printStackTrace();
         }

   }
   
   @Override
   public String toString(){
      return "AOF["+cache+"]";
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

}
