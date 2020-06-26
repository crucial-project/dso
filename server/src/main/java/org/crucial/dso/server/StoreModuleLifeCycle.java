package org.crucial.dso.server;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.lifecycle.AbstractModuleLifecycle;
import org.infinispan.marshall.core.ExternallyMarshallable;

public class StoreModuleLifeCycle extends AbstractModuleLifecycle {

    @Override
    public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
        ExternallyMarshallable.addToWhiteList("org.crucial.dso");
    }

    @Override
    public void cacheManagerStarted(GlobalComponentRegistry gcr) {}

    @Override
    public void cacheStarted(ComponentRegistry cr, String cacheName) {}
}
