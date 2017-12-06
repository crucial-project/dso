package org.infinispan.creson.utils;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.test.AbstractCacheTest;

/**
 * @author Pierre Sutra
 */
public class ConfigurationHelper {

    public static ConfigurationBuilder buildConfiguration(
            CacheMode mode,
            int replicationFactor,
            long maxEntries,
            String storagePath) {
        return buildConfiguration(mode, replicationFactor, maxEntries, storagePath, true);
    }

    public static ConfigurationBuilder buildConfiguration(
            CacheMode mode,
            int replicationFactor,
            long maxEntries,
            String storagePath,
            boolean purgeOnStartup) {

        ConfigurationBuilder builder;

        builder
                = AbstractCacheTest.getDefaultClusteredCacheConfig(mode, false);
        builder.clustering()
                .stateTransfer().fetchInMemoryState(true)
                .stateTransfer().chunkSize(Integer.MAX_VALUE)
                .awaitInitialTransfer(true)
                .hash().numOwners(replicationFactor);

        if (maxEntries >= 0) {
            builder.memory().size(maxEntries);
            SingleFileStoreConfigurationBuilder storeConfigurationBuilder
                    = builder.persistence().addSingleFileStore();
            storeConfigurationBuilder.location(storagePath);
            storeConfigurationBuilder.purgeOnStartup(purgeOnStartup);
            storeConfigurationBuilder.fetchPersistentState(false);
            storeConfigurationBuilder.persistence().passivation(true);
            storeConfigurationBuilder.compatibility().enabled(true);
        }

        return builder;
    }

}
