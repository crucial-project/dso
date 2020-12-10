package org.crucial.dso.test;

import org.crucial.dso.utils.ConfigurationHelper;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.commons.marshall.Marshaller;
import org.crucial.dso.Factory;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.crucial.dso.Factory.DSO_CACHE_NAME;
import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;
import static org.testng.Assert.assertEquals;

/**
 * @author Pierre Sutra
 */

@Test(testName = "RemoteTest")
public class RemoteTest extends AbstractTest {

    private static List<HotRodServer> servers = new ArrayList<>();
    private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();

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
        GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
        gbuilder.serialization()
                .marshaller(new JavaSerializationMarshaller())
                .whiteList()
                .addRegexps(".*");
        TransportFlags flags = new TransportFlags();
        flags.withFD(true).withMerge(true);
        EmbeddedCacheManager cm = TestCacheManagerFactory.createClusteredCacheManager(
                false,
                gbuilder,
                (ConfigurationBuilder)null,
                flags);
        this.cacheManagers.add(cm);
        ConfigurationHelper.installCache(
                cm,
                CACHE_MODE,
                REPLICATION_FACTOR,
                MAX_ENTRIES,
                PASSIVATION,
                PERSISTENT_STORAGE_DIR + "/" + index,
                true,
                false,
                true);
        waitForClusterToForm(DSO_CACHE_NAME);

        // hotrod server
        HotRodServer server = HotRodTestingUtil.startHotRodServer(
                manager(index),
                11222 + index);
        servers.add(server);

        // remote manager
        RemoteCacheManager manager = new RemoteCacheManager(
                new org.infinispan.client.hotrod.configuration.ConfigurationBuilder()
                        .addServers(server.getHost() + ":" + server.getPort())
                        .marshaller(new JavaSerializationMarshaller()).addJavaSerialWhiteList(".*")
                        .forceReturnValues(true)
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

        for (int j = 0; j < NMANAGERS; j++) {
            addContainer();
        }

        // Verify that default caches are started.
        for (int j = 0; j < NMANAGERS; j++) {
            blockUntilCacheStatusAchieved(
                    manager(j).getCache(DSO_CACHE_NAME), ComponentStatus.RUNNING, 10000);
        }

        waitForClusterToForm(DSO_CACHE_NAME);

        assertEquals(manager(0).getTransport().getMembers().size(), NMANAGERS);

        Factory.forCache(container(0).getCache(DSO_CACHE_NAME), true);
    }

}
