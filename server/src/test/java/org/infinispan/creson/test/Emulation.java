package org.infinispan.creson.test;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.creson.Factory;
import org.infinispan.creson.utils.ConfigurationHelper;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;

public class Emulation extends MultipleCacheManagersTest {

    protected static final CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
    protected static final long MAX_ENTRIES = 0;
    protected static final int REPLICATION_FACTOR = 1;
    protected static final String PERSISTENT_STORAGE_DIR = "/tmp/creson-storage";

    protected int numberOfContainers(){
        return 1;
    }

    private ConfigurationBuilder buildConfiguration() {
        return ConfigurationHelper.buildConfiguration(
                CACHE_MODE,
                REPLICATION_FACTOR,
                MAX_ENTRIES,
                PERSISTENT_STORAGE_DIR + "/" + this.cacheManagers.size());
    }

    private void addContainer(){
        TransportFlags flags = new TransportFlags();
        flags.withFD(true).withMerge(true);
        EmbeddedCacheManager cm = addClusterEnabledCacheManager(buildConfiguration(), flags);
        waitForClusterToForm(CRESON_CACHE_NAME);
        Cache cache = cm.getCache(CRESON_CACHE_NAME);
        Factory.forCache(cache);
        System.out.println("Cache manager "+ cm+" ready.");
    }

    @Override
    protected void createCacheManagers() throws Throwable {
        for (int i=0; i<numberOfContainers(); i++) {
            addContainer();
        }
    }
}
