package org.infinispan.creson.server;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.interceptors.impl.CallInterceptor;
import org.infinispan.lifecycle.AbstractModuleLifecycle;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.ExternallyMarshallable;
import org.infinispan.registry.InternalCacheRegistry;

import java.util.EnumSet;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;

public class CresonModuleLifeCycle extends AbstractModuleLifecycle {

    @Override
    public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
        ExternallyMarshallable.addToWhiteList("org.infinispan.creson");
    }

    @Override
    public void cacheManagerStarted(GlobalComponentRegistry gcr) {
        final EmbeddedCacheManager cacheManager = gcr.getComponent(EmbeddedCacheManager.class);
        final InternalCacheRegistry registry= gcr.getComponent(InternalCacheRegistry.class);

        Interceptor interceptor = new Interceptor(cacheManager);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.read(cacheManager.getDefaultCacheConfiguration());
        builder.compatibility().enabled(true); // FIXME for HotRod
        builder.clustering().stateTransfer().awaitInitialTransfer(false); // FIXME interceptor is reentrant
        builder.customInterceptors().addInterceptor().before(CallInterceptor.class).interceptor(interceptor);

        registry.registerInternalCache(
                CRESON_CACHE_NAME,
                builder.build(),
                EnumSet.of(
                        InternalCacheRegistry.Flag.EXCLUSIVE,
                        InternalCacheRegistry.Flag.PERSISTENT,
                        InternalCacheRegistry.Flag.USER));

    }

}
