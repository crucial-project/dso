package org.infinispan.creson.test;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;

import java.util.ArrayList;
import java.util.List;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;
import static org.infinispan.creson.utils.ConfigurationHelper.installCreson;

public class Emulation extends MultipleCacheManagersTest {

    protected static final CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
    protected static final int REPLICATION_FACTOR = 1;
    protected static final String PERSISTENT_STORAGE_DIR = "/tmp/creson-storage";

    private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();

    protected int numberOfCaches(){
        return 1;
    }

    private static List<HotRodServer> servers = new ArrayList<>();

    private boolean addContainer() {
        int index = servers.size();

        // embedded cache manager
        TransportFlags flags = new TransportFlags();
        flags.withFD(true).withMerge(true);
        EmbeddedCacheManager cm = addClusterEnabledCacheManager(flags);
        installCreson(
                cm,
                CACHE_MODE,
                REPLICATION_FACTOR,
                0,
                PERSISTENT_STORAGE_DIR + "/" + index,
                true,
                false);
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
        for (int i = 0; i< numberOfCaches(); i++) {
            addContainer();
            waitForClusterToForm();
            System.out.println("Node " + remoteCacheManagers.get(i)+ " added.");
        }
    }
}
