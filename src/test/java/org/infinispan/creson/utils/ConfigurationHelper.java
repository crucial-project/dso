package org.infinispan.creson.utils;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;

import static org.infinispan.test.AbstractCacheTest.getDefaultClusteredCacheConfig;

/**
 * Created by otrack on 01/07/17.
 */
public class ConfigurationHelper {

    public static ConfigurationBuilder buildConfiguration(
            CacheMode mode,
            int replicationFactor,
            long maxEntries,
            String storagePath) {
        return buildConfiguration(mode, replicationFactor, maxEntries, storagePath, false);
    }

    public static ConfigurationBuilder buildConfiguration(
            CacheMode mode,
            int replicationFactor,
            long maxEntries,
            String storagePath,
            boolean purgeOnStartup) {

        ConfigurationBuilder builder;

        builder
                = getDefaultClusteredCacheConfig(mode, false);
        builder
                .clustering().hash().numOwners(replicationFactor);

        if (maxEntries != Long.MAX_VALUE) {
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
