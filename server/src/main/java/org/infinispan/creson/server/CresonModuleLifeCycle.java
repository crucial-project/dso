package org.infinispan.creson.server;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.creson.Factory;
import org.infinispan.creson.Server;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.interceptors.impl.CallInterceptor;
import org.infinispan.lifecycle.AbstractModuleLifecycle;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.ExternallyMarshallable;
import org.infinispan.registry.InternalCacheRegistry;

import java.util.EnumSet;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;

public class CresonModuleLifeCycle extends AbstractModuleLifecycle {

    StateMachineInterceptor stateMachineInterceptor;

    @Override
    public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
        ExternallyMarshallable.addToWhiteList("org.infinispan.creson");
    }

    @Override
    public void cacheManagerStarted(GlobalComponentRegistry gcr) {
        final EmbeddedCacheManager cacheManager = gcr.getComponent(EmbeddedCacheManager.class);
        final InternalCacheRegistry registry= gcr.getComponent(InternalCacheRegistry.class);
        stateMachineInterceptor = new StateMachineInterceptor();

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.read(cacheManager.getDefaultCacheConfiguration());
        /*builder.indexing().index(Index.LOCAL);
        for(Class cls : Server.getIndexedClass()) {
            builder.indexing().addIndexedEntity(cls);
        }*/

        builder.compatibility().enabled(true); // for HotRod
        builder.clustering().stateTransfer().awaitInitialTransfer(true);
        builder.customInterceptors().addInterceptor().before(CallInterceptor.class).interceptor(stateMachineInterceptor);

        registry.registerInternalCache(
                CRESON_CACHE_NAME,
                builder.build(),
                EnumSet.of(
                        InternalCacheRegistry.Flag.EXCLUSIVE,
                        InternalCacheRegistry.Flag.PERSISTENT,
                        InternalCacheRegistry.Flag.USER));

    }

    @Override
    public void cacheStarted(ComponentRegistry cr, String cacheName) {
        if (cacheName.equals(CRESON_CACHE_NAME)) {
            Cache cache = cr.getComponent(org.infinispan.Cache.class);
            Factory factory = Factory.forCache(cache);
            stateMachineInterceptor.setup(factory);
        }
    }
}
