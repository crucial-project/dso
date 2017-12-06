package org.infinispan.creson.test;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.creson.utils.ConfigurationHelper;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.MultipleCacheManagersTest;

import java.util.ArrayList;
import java.util.List;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;

public class Emulation extends MultipleCacheManagersTest {

    protected static final CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
    protected static final long MAX_ENTRIES = 0;
    protected static final int REPLICATION_FACTOR = 1;
    protected static final String PERSISTENT_STORAGE_DIR = "/tmp/creson-storage";

    private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();

    protected int numberOfContainers(){
        return 1;
    }

    private static List<HotRodServer> servers = new ArrayList<>();

    private ConfigurationBuilder buildConfiguration() {
        return ConfigurationHelper.buildConfiguration(
                CACHE_MODE,
                REPLICATION_FACTOR,
                MAX_ENTRIES,
                PERSISTENT_STORAGE_DIR + "/" + this.cacheManagers.size());
    }

    private boolean addContainer() {
        int index = servers.size();

        // embedded cache manager
        addClusterEnabledCacheManager(buildConfiguration()).getCache(CRESON_CACHE_NAME);
        waitForClusterToForm(CRESON_CACHE_NAME);

        // hotrod server
        HotRodServer server = HotRodTestingUtil.startHotRodServer(
                manager(index),
                11222 + index);
        servers.add(server);

        // remote manager
        RemoteCacheManager manager = new RemoteCacheManager(
                new org.infinispan.client.hotrod.configuration.ConfigurationBuilder()
                        .addServers(server.getHost() + ":" + server.getPort())
                        .marshaller((Marshaller) null)
                        .forceReturnValues(true)
                        .build());

        remoteCacheManagers.add(manager);
        return true;
    }

    @Override
    protected void createCacheManagers() throws Throwable {
        for (int i=0; i<numberOfContainers(); i++) {
            addContainer();
            waitForClusterToForm();
            System.out.println("Node " + remoteCacheManagers.get(i)+ " added.");
        }
    }
}
