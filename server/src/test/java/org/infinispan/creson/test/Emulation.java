package org.infinispan.creson.test;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.MultipleCacheManagersTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;
import static org.infinispan.creson.utils.ConfigurationHelper.installCreson;
import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;

@Test(testName = "emulation")
public class Emulation extends MultipleCacheManagersTest {

    protected static List<HotRodServer> servers = new ArrayList<>();
    protected static List<RemoteCacheManager> remoteCacheManagers = new ArrayList<>();

    protected int clusterSize(){
        return 3;
    }

    public boolean addContainer() {
        int index = servers.size();

        // embedded cache manager
        EmbeddedCacheManager cm = addClusterEnabledCacheManager();
        installCreson(
                cm,
                CacheMode.DIST_ASYNC,
                1,
                -1,
                "",
                false,
                true);
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

        System.out.println("Node " + manager + " added.");
        return true;
    }

    public void deleteContainer() {

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

    }


    @Override
    protected void createCacheManagers() throws Throwable {

        for (int j = 0; j < clusterSize(); j++) {
            addContainer();
        }

        for (int j = 0; j < clusterSize(); j++) {
            blockUntilCacheStatusAchieved(
                    manager(j).getCache(CRESON_CACHE_NAME), ComponentStatus.RUNNING, 10000);
        }

        waitForClusterToForm();

    }

}
