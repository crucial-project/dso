package org.crucial.dso;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.crucial.dso.container.AbstractContainer;
import org.crucial.dso.object.Reference;
import org.crucial.dso.utils.ContextManager;
import org.crucial.dso.utils.Reflection;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.crucial.dso.container.BaseContainer;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pierre Sutra
 */
public class Factory {

    private static Log log = LogFactory.getLog(Factory.class);
    private static Factory singleton;
    private static Map<BasicCache, Factory> factories = new HashMap<>();

    // Server
    public static final String DSO_SERVER_DEFAULT = "127.0.0.1:11222";
    public static final String DSO = "DSO"; // env var
    public static final String CONFIG_FILE = "config.properties"; // property
    public static final String DSO_SERVER = "dso.server";

    // Cache
    public static final String DSO_CACHE_NAME = "__dso";

    // Internal interface

    @Deprecated
    public synchronized static Factory forCache(BasicCache cache) {
        return Factory.forCache(cache, MAX_CONTAINERS, false);
    }

    @Deprecated
    public synchronized static Factory forCache(BasicCache cache, boolean force) {
        return Factory.forCache(cache, MAX_CONTAINERS, force);
    }

    @Deprecated
    public synchronized static Factory forCache(BasicCache cache, int maxContainers, boolean force) {
        assert !(cache instanceof RemoteCache) ||
                ((RemoteCache)cache).getRemoteCacheManager().getConfiguration().forceReturnValues();

        if (!factories.containsKey(cache))
            factories.put(cache, new Factory(cache, maxContainers));

        if ((singleton == null | force) && cache.getName().equals(DSO_CACHE_NAME)) {
            singleton = factories.get(cache);
            log.info("Factory singleton  is " + singleton);
        }

        return factories.get(cache);
    }

    @Deprecated
    public static Factory getSingleton() {
        if (singleton==null) {
            singleton = Factory.get();
        }
        return singleton;
    }

    // Preferred interface

    public static Factory get() {
        String server = System.getenv(DSO);
        if (server == null) {
            Properties properties = System.getProperties();
            try (InputStream is = Factory.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                properties.load(is);
                server = properties.containsKey(DSO_SERVER) ?
                        properties.getProperty(DSO_SERVER) : DSO_SERVER_DEFAULT;
            } catch (Exception e) {
                // ignore
            }
        }
        if (server == null) {
            log.warn("Using default server: " + DSO_SERVER_DEFAULT);
            server = DSO_SERVER_DEFAULT;
        }
        return get(server);
    }

    public static Factory get(String server, long seed) {
        ContextManager.seedGenerator(seed);
        return get(server);
    }

    public static Factory get(String server) {
        int port = Integer.valueOf(server.split(":")[1]);
        String host = server.split(":")[0];
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
                = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        cb.tcpNoDelay(true)
                .clientIntelligence(ClientIntelligence.BASIC)
                .addServer()
                .host(host)
                .port(port)
                .forceReturnValues(true)
                .addJavaSerialWhiteList(".*")
                .marshaller(new JavaSerializationMarshaller()).addJavaSerialWhiteList(".*")
                .connectionTimeout(3000)
                .maxRetries(5);
        RemoteCacheManager manager = new RemoteCacheManager(cb.build());
        return forCache(manager.getCache(DSO_CACHE_NAME));
    }


    protected static final int MAX_CONTAINERS = Integer.MAX_VALUE;

    // Object fields

    private BasicCache cache;
    private final ConcurrentMap<Reference, AbstractContainer> registeredContainers;
    private int maxSize;

    /**
     * Return an Factory built on top of cache <i>c</i>.
     *
     * @param c a cache,  key must be synchronous.and non-transactional
     */
    private Factory(BasicCache<Object, Object> c) throws CacheException {
        this(c, MAX_CONTAINERS);
    }

    /**
     * Returns an object factory built on top of cache <i>c</i> with a bounded amount <i>m</i> of
     * containers in key. Upon the removal of a container, the object is stored persistently in the cache.
     *
     * @param c key must be synchronous.and non-transactional
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

    public BasicCache getCache(){
        return this.cache;
    }

    @Deprecated
    public <T> T getInstanceOf(Class clazz) throws CacheException {
        return (T) getInstanceOf(clazz, null, false, false, false);
    }

    @Deprecated
    public <T> T getInstanceOf(Reference reference) throws CacheException {
        return (T) getInstanceOf(reference.getClazz(), reference.getKey(), false, false, false);
    }

    @Deprecated
    public <T> T getInstanceOf(Class clazz, Object key) throws CacheException {
        return (T) getInstanceOf(clazz, key, false, false, false);
    }

    /**
     * Returns an object of class <i>clazz</i>.
     * The class of this object must be initially serializable, as well as all the parameters of its methods.
     * Furthermore, the class must be deterministic.
     * <p>
     * The object is atomic if <i>withReadOptimization</i> equals false; otherwise key is sequentially consistent..
     * In more details, if <i>withReadOptimization</i>  is set, every call to the object is first executed locally on a copy of the object, and in case
     * the call does not modify the state of the object, the value returned is the result of this tentative execution.
     *
     * @param clazz                a class object
     * @param withReadOptimization set the read optimization on/off.
     * @return an object of the class <i>clazz</i>
     * @throws CacheException
     */
    @Deprecated
    public <T> T getInstanceOf(Class<T> clazz, boolean withReadOptimization)
            throws CacheException {
        return getInstanceOf(clazz, null, withReadOptimization, false, false, false);
    }

    /**
     * Returns an object of class <i>clazz</i>.
     * The class of this object must be initially serializable, as well as all the parameters of its methods.
     * Furthermore, the class must be deterministic.
     * <p>
     * The object is atomic if <i>withReadOptimization</i> equals false; otherwise key is sequentially consistent..
     * In more details, if <i>withReadOptimization</i>  is set, every call to the object is executed locally on a copy of the object, and in case
     * the call does not modify the state of the object, the value returned is the result of this tentative execution.
     * If the method <i>equalsMethod</i>  is not null, key overrides the default <i>clazz.equals()</i> when testing that the state of the object and
     * its copy are identical.
     *
     * @param clazz                a class object
     * @param withReadOptimization set the read optimization on/off.
     * @param withIdempotence      set idempotence on/off.
     * @param forceNew             force the creation of the object, even if key exists already in the cache
     * @return an object of the class <i>clazz</i>
     * @throws CacheException
     */
    @Deprecated
    public <T> T getInstanceOf(Class<T> clazz, Object key, boolean withReadOptimization, boolean withIdempotence,
                               boolean forceNew, Object... initArgs) throws CacheException {

        Reference reference;
        AbstractContainer container = null;

        try {

            reference = new Reference(clazz, key);

            if (forceNew) {
                registeredContainers.remove(reference);
            } else {
                container = registeredContainers.get(reference);
            }

            if (container == null) {
                try {
                    Reflection.getConstructor(clazz,initArgs);
                } catch (Exception e) {
                    throw new CacheException(clazz + " no constructor with "+ Arrays.toString(initArgs));
                }
                container = new BaseContainer(cache, clazz, key, withReadOptimization, withIdempotence, forceNew, initArgs);
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

    }

    public void close() {
        log.info("closed");
    }

    @Override
    public String toString() {
        return "Factory[" + cache.toString() + "]";
    }

    public void clear(){
        log.info("cleared");
        cache.clear();
    }

}
