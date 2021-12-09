package org.crucial.dso.server;

import org.crucial.dso.Factory;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.InfinispanModule;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.interceptors.impl.CallInterceptor;
import org.infinispan.lifecycle.ModuleLifecycle;
import org.infinispan.registry.InternalCacheRegistry;
import org.infinispan.util.logging.Log;

@InfinispanModule(name = "crucial-dso", requiredModules = "core")
public class DSOLifecycle implements ModuleLifecycle {
    private DSOConfiguration configuration;
    private StateMachineInterceptor stateMachineInterceptor;

    @Override
    public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
        BasicComponentRegistry registry = gcr.getComponent(BasicComponentRegistry.class);
        InternalCacheRegistry internalCacheRegistry = registry.getComponent(InternalCacheRegistry.class).running();
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.DIST_ASYNC);
        internalCacheRegistry.registerInternalCache(Factory.DSO_CACHE_NAME, builder.build());
        configuration = globalConfiguration.module(DSOConfiguration.class);
        if (configuration == null) {
            configuration = new DSOConfigurationBuilder().create();
        }
    }

    @Override
    public void cacheStarting(ComponentRegistry cr, Configuration configuration, String cacheName) {
        if (Factory.DSO_CACHE_NAME.equals(cacheName) && configuration.clustering().cacheMode().isClustered()) {
            BasicComponentRegistry bcr = cr.getComponent(BasicComponentRegistry.class);
            stateMachineInterceptor = new StateMachineInterceptor();
            bcr.registerComponent(StateMachineInterceptor.class, stateMachineInterceptor, true);
            bcr.addDynamicDependency(AsyncInterceptorChain.class.getName(), StateMachineInterceptor.class.getName());
            bcr.getComponent(AsyncInterceptorChain.class).wired()
                    .addInterceptorAfter(stateMachineInterceptor, CallInterceptor.class);
        }
    }

    @Override
    public void cacheStarted(ComponentRegistry cr, String cacheName) {
        if (Factory.DSO_CACHE_NAME.equals(cacheName)) {
            stateMachineInterceptor.setup(Factory.forCache(cr.getCache().running()), this.configuration.idempotent());
            Log.CONTAINER.info("DSO cache initialized");
        }
    }
}
