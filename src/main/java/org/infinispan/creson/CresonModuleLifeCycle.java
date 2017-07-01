package org.infinispan.creson;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.creson.interceptor.Interceptor;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.interceptors.impl.CallInterceptor;
import org.infinispan.lifecycle.AbstractModuleLifecycle;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.ExternallyMarshallable;
import org.infinispan.registry.InternalCacheRegistry;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;

import java.util.EnumSet;

public class CresonModuleLifeCycle extends AbstractModuleLifecycle {

    public static final String CRESON_CACHE_NAME = "__creson";

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
        builder.clustering()
                .locking().isolationLevel(IsolationLevel.SERIALIZABLE)
                .clustering().l1().disable()
                .locking().useLockStriping(false)
                .compatibility().enabled(true)
                .transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL)
                .customInterceptors().addInterceptor().before(CallInterceptor.class).interceptor(interceptor);

        registry.registerInternalCache(
                CRESON_CACHE_NAME,
                builder.build(),
                EnumSet.of(
                        InternalCacheRegistry.Flag.EXCLUSIVE,
                        InternalCacheRegistry.Flag.PERSISTENT,
                        InternalCacheRegistry.Flag.USER));

    }

}
