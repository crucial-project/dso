package org.infinispan.creson;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.creson.container.AbstractContainer;
import org.infinispan.creson.container.BaseContainer;
import org.infinispan.creson.object.Reference;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.infinispan.creson.object.Reference.unreference;
import static org.infinispan.creson.utils.Reflection.constructor;

/**
 * @author Pierre Sutra
 */
public class Factory {

    // Class fields
    public static final String CRESON_CACHE_NAME = "__creson";

    private static Log log = LogFactory.getLog(Factory.class);
    private static Factory singleton;
    private static Map<BasicCache, Factory> factories = new HashMap<>();


    public static Factory getSingleton() {
        assert singleton != null;
        return singleton;
    }

    public synchronized static Factory forCache(BasicCache cache) {
        return Factory.forCache(cache, MAX_CONTAINERS);
    }

    public synchronized static Factory forCache(BasicCache cache, int maxContainers) {
        if (!factories.containsKey(cache))
            factories.put(cache, new Factory(cache, maxContainers));

        if (singleton == null && cache.getName().equals(CRESON_CACHE_NAME)) {
            singleton = factories.get(cache);
            log.info("AOF singleton  is " + singleton);
        }

        return factories.get(cache);
    }

    protected static final int MAX_CONTAINERS = Integer.MAX_VALUE;

    // Object fields

    private BasicCache cache;
    private final ConcurrentMap<Reference, AbstractContainer> registeredContainers;
    private int maxSize;

    /**
     * Return an Factory built on top of cache <i>c</i>.
     *
     * @param c a cache,  it must be synchronous.and non-transactional
     */
    private Factory(BasicCache<Object, Object> c) throws CacheException {
        this(c, MAX_CONTAINERS);
    }

    /**
     * Returns an object factory built on top of cache <i>c</i> with a bounded amount <i>m</i> of
     * containers in it. Upon the removal of a container, the object is stored persistently in the cache.
     *
     * @param c it must be synchronous.and non-transactional
     * @param m max amount of containers kept by this factory.
     * @throws CacheException
     */
    private Factory(BasicCache<Object, Object> c, int m) throws CacheException {
        cache = c;
        maxSize = m;
        registeredContainers = CacheBuilder.newBuilder()
                .maximumSize(MAX_CONTAINERS)
                .removalListener((RemovalListener<Reference, AbstractContainer>) notifiication -> {
                    try {
                        disposeInstanceOf(notifiication.getValue().getReference());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .build().asMap();
        log.info(this + " Created");
    }

    public static Factory get(String server) {
        int port = Integer.valueOf(server.split(":")[1]);
        String host = server.split(":")[0];
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
                = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        cb.tcpNoDelay(true)
                .addServer()
                .host(host)
                .port(port);
        RemoteCacheManager manager = new RemoteCacheManager(cb.build());
        return forCache(manager.getCache(CRESON_CACHE_NAME));
    }

    public <T> T getInstanceOf(Class clazz) throws CacheException {
        return (T) getInstanceOf(clazz, null, false, false);
    }

    public <T> T getInstanceOf(Reference reference) throws CacheException {
        return (T) getInstanceOf(reference.getClazz(), reference.getKey(), false, false);
    }

    public <T> T getInstanceOf(Class clazz, Object key) throws CacheException {
        return (T) getInstanceOf(clazz, key, false, false);
    }

    /**
     * Returns an object of class <i>clazz</i>.
     * The class of this object must be initially serializable, as well as all the parameters of its methods.
     * Furthermore, the class must be deterministic.
     * <p>
     * The object is atomic if <i>withReadOptimization</i> equals false; otherwise it is sequentially consistent..
     * In more details, if <i>withReadOptimization</i>  is set, every call to the object is first executed locally on a copy of the object, and in case
     * the call does not modify the state of the object, the value returned is the result of this tentative execution.
     *
     * @param clazz                a class object
     * @param withReadOptimization set the read optimization on/off.
     * @return an object of the class <i>clazz</i>
     * @throws CacheException
     */
    public <T> T getInstanceOf(Class<T> clazz, boolean withReadOptimization)
            throws CacheException {
        return getInstanceOf(clazz, null, withReadOptimization, false);
    }

    /**
     * Returns an object of class <i>clazz</i>.
     * The class of this object must be initially serializable, as well as all the parameters of its methods.
     * Furthermore, the class must be deterministic.
     * <p>
     * The object is atomic if <i>withReadOptimization</i> equals false; otherwise it is sequentially consistent..
     * In more details, if <i>withReadOptimization</i>  is set, every call to the object is executed locally on a copy of the object, and in case
     * the call does not modify the state of the object, the value returned is the result of this tentative execution.
     * If the method <i>equalsMethod</i>  is not null, it overrides the default <i>clazz.equals()</i> when testing that the state of the object and
     * its copy are identical.
     *
     * @param clazz                a class object
     * @param withReadOptimization set the read optimization on/off.
     * @param forceNew             force the creation of the object, even if it exists already in the cache
     * @return an object of the class <i>clazz</i>
     * @throws CacheException
     */
    public <T> T getInstanceOf(Class<T> clazz, Object key, boolean withReadOptimization, boolean forceNew, Object... initArgs)
            throws CacheException {

        if (HashMap.class.isAssignableFrom(clazz)) {
            assert key != null; // can only occur for static distributed field
            return (T) new DistributedObjectsMap(this, new Reference(clazz, key), cache);
        }

        if (!(Serializable.class.isAssignableFrom(clazz))) {
            throw new CacheException(clazz + " should be serializable.");
        }

        try {
            constructor(clazz,initArgs);
        } catch (Exception e) {
            throw new CacheException(clazz + " no constructor with "+ Arrays.toString(initArgs));
        }

        Reference reference;
        AbstractContainer container = null;

        try {

            if (key != null) {
                reference = new Reference(clazz, key);
                container = registeredContainers.get(reference);
            }

            if (container == null) {
                container = new BaseContainer(cache, clazz, key, withReadOptimization, forceNew, initArgs);
                reference = container.getReference();
                if (registeredContainers.putIfAbsent(reference, container) == null) {
                    if (log.isTraceEnabled())
                        log.trace(this + " adding " + container + " with " + container.getReference());
                }
                container = registeredContainers.get(reference);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new CacheException(e.getCause());
        }

        assert container != null;

        return (T) container.getProxy();

    }


    @Deprecated
    public void disposeInstanceOf(Reference reference)
            throws CacheException {
        disposeInstanceOf(reference.getClazz(), reference.getKey());
    }

    @Deprecated
    public void disposeInstanceOf(Class clazz, Object key)
            throws CacheException {

        Reference reference = new Reference<>(clazz, key);

        AbstractContainer container = registeredContainers.get(reference);

        if (container == null) return;

        if (log.isDebugEnabled())
            log.debug(" disposing " + container);

        registeredContainers.remove(reference);

        try {
            container.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CacheException("Error while closing  " + container);
        }

    }

    public void close() {
        for (AbstractContainer container : registeredContainers.values())
            disposeInstanceOf(container.getReference());
        log.info("closed");
    }

    @Override
    public String toString() {
        return "Factory[" + cache.toString() + "]";
    }

    // Helpers

    public static class DistributedObjectsMap implements Map {

        private Factory factory;
        private Reference reference;
        private BasicCache cache;

        public DistributedObjectsMap(Factory factory, Reference reference, BasicCache cache) {
            this.factory = factory;
            this.reference = reference;
            this.cache = cache;
        }

        // FIXME use instead an object pairing (reference, key)
        private Object transformKey(Object key) {
            return reference.toString() + "#" + key.toString(); // portable
        }

        @Override
        public Object get(Object key) {
            Object object = cache.get(transformKey(key));
            if (object instanceof Reference)
                return unreference((Reference) object, factory);
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
            for (Map.Entry entry : entries) {
                put(transformKey(entry.getKey()), entry.getValue());
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
        public String toString() {
            return cache.toString();
        }

    }

}
