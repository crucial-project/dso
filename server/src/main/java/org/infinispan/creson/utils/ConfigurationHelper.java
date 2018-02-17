package org.infinispan.creson.utils;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.creson.Factory;
import org.infinispan.creson.server.StateMachineInterceptor;
import org.infinispan.interceptors.impl.CallInterceptor;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;

/**
 * @author Pierre Sutra
 */
public class ConfigurationHelper {

    public static void installCreson(
            EmbeddedCacheManager manager,
            CacheMode mode,
            int replicationFactor,
            long maxEntries,
            boolean withPassivation,
            String storagePath,
            boolean purge,
            boolean withIndexing) {

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(mode);
        builder.transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL);
        builder.compatibility().enabled(true); // for HotRod
        builder.expiration().lifespan(-1);
        builder.memory().size(maxEntries);

        StateMachineInterceptor stateMachineInterceptor = new StateMachineInterceptor();
        builder.customInterceptors().addInterceptor().before(CallInterceptor.class).interceptor(stateMachineInterceptor);

        // clustering
        builder.clustering()
                .stateTransfer().fetchInMemoryState(true)
                .stateTransfer().chunkSize(Integer.MAX_VALUE) // FIXME necessary for elasticity.
                .awaitInitialTransfer(true)
                .hash().numOwners(replicationFactor);

        // indexing
        if (withIndexing) {
            builder.indexing().index(Index.LOCAL)
                    .addProperty("default.directory_provider", "ram")
                    .addProperty("lucene_version", "LUCENE_CURRENT");
        }

        // persistence
        if (withPassivation) {
            SingleFileStoreConfigurationBuilder storeConfigurationBuilder
                    = builder.persistence().addSingleFileStore();
            storeConfigurationBuilder.location(storagePath);
            storeConfigurationBuilder.persistence().passivation(true); // no write-through
            storeConfigurationBuilder.fetchPersistentState(true);
            storeConfigurationBuilder.purgeOnStartup(purge);
        }

        // installation
        manager.defineConfiguration(CRESON_CACHE_NAME,builder.build());
        stateMachineInterceptor.setup(Factory.
                forCache(manager.getCache(CRESON_CACHE_NAME)));

    }

}
