package org.infinispan.creson;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;
import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;
import static org.testng.Assert.assertEquals;

/**
 * @author Pierre Sutra
 */
@Test(testName = "RemoteTest", groups = "unit")
public class RemoteTest extends AbstractTest {

    private static List<HotRodServer> servers = new ArrayList<>();
    private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();
    private static ConfigurationBuilder defaultBuilder;

    @Override
    public BasicCacheContainer container(int i) {
        return remoteCacheManagers.get(i);
    }

    @Override
    public Collection<BasicCacheContainer> containers() {
        return remoteCacheManagers;
    }

    @Override
    public boolean addContainer() {
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
                        .build());
        remoteCacheManagers.add(manager);

        System.out.println("Node " + manager + " added.");
        return true;
    }

    @Override
    public boolean deleteContainer() {
        if (servers.size() == 0) return false;
        int index = servers.size() - 1;

        // remote manager
        remoteCacheManagers.get(index).stop();
        remoteCacheManagers.remove(index);

        // hotrod server
        servers.get(index).stop();
        servers.remove(index);

        // embedded cache manager
        BasicCacheContainer manager = cacheManagers.get(index);
        cacheManagers.get(index).stop();
        cacheManagers.remove(index);

        waitForClusterToForm();
        System.out.println("Node " + manager + " deleted.");

        return true;
    }


    @Override
    protected void createCacheManagers() throws Throwable {
        createDefaultBuilder();

        for (int j = 0; j < NMANAGERS; j++) {
            addContainer();
        }

        // Verify that default caches are started.
        for (int j = 0; j < NMANAGERS; j++) {
            blockUntilCacheStatusAchieved(
                    manager(j).getCache(CRESON_CACHE_NAME), ComponentStatus.RUNNING, 10000);
        }

        waitForClusterToForm();

        assertEquals(manager(0).getTransport().getMembers().size(), NMANAGERS);

        Factory.forCache(container(0).getCache(CRESON_CACHE_NAME));
    }

    // Helpers

    private void createDefaultBuilder() {
        defaultBuilder = getDefaultClusteredCacheConfig(CACHE_MODE, false);
        defaultBuilder
                .clustering().cacheMode(CacheMode.DIST_SYNC).hash().numOwners(REPLICATION_FACTOR)
                .locking().useLockStriping(false)
                .compatibility().enable();
        defaultBuilder.clustering().stateTransfer()
                .awaitInitialTransfer(true)
                .timeout(10000)
                .fetchInMemoryState(true);
    }

}
