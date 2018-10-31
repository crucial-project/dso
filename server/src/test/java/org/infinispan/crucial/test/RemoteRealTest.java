package org.infinispan.crucial.test;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.crucial.Factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.infinispan.crucial.Factory.CRUCIAL_CACHE_NAME;

/**
 * @author Pierre Sutra
 */

public abstract class RemoteRealTest extends AbstractTest {

    private static List<BasicCacheContainer> basicCacheContainers = new ArrayList<>();

    @Override
    public BasicCacheContainer container(int i) {
        return basicCacheContainers.get(i % basicCacheContainers.size());
    }

    @Override
    public Collection<BasicCacheContainer> containers() {
        return basicCacheContainers;
    }

    @Override
    public boolean addContainer() {
        return false;
    }

    @Override
    public boolean deleteContainer() {
        return false;
    }

    @Override
    protected void createCacheManagers() throws Throwable {

        for (String server : servers()) {
            System.out.println("adding "+server);
            String host = server.split(":")[0];
            int port = Integer.valueOf(server.split(":")[1]);
            org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
                    = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
            cb.addServer().host(host).port(port).forceReturnValues(true);
            RemoteCacheManager manager = new RemoteCacheManager(cb.build());
            manager.start();
            manager.getCache(CRUCIAL_CACHE_NAME).clear();
            basicCacheContainers.add(manager);
        }

        this.cleanup = null;
        Factory.forCache(basicCacheContainers.get(0).getCache(CRUCIAL_CACHE_NAME));
    }

    @Override
    protected void clearContent() throws Throwable {
        for (BasicCacheContainer container : basicCacheContainers) {
            container.getCache(CRUCIAL_CACHE_NAME).clear();
        }
    }

    protected String[] servers() {
        return new String[]{"localhost:11222"};
    }

}
