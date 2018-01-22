package org.infinispan.creson.server;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.lifecycle.AbstractModuleLifecycle;
import org.infinispan.marshall.core.ExternallyMarshallable;

public class CresonModuleLifeCycle extends AbstractModuleLifecycle {

    StateMachineInterceptor stateMachineInterceptor;

    @Override
    public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalConfiguration) {
        ExternallyMarshallable.addToWhiteList("org.infinispan.creson");
    }

    @Override
    public void cacheManagerStarted(GlobalComponentRegistry gcr) {
    }

    @Override
    public void cacheStarted(ComponentRegistry cr, String cacheName) {
    }
}
