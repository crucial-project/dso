package org.infinispan.creson.server;

import org.infinispan.AdvancedCache;
import org.infinispan.server.core.QueryFacade;

public class QueryFacadeImpl implements QueryFacade {

    @Override
    public byte[] query(AdvancedCache<byte[], byte[]> cache, byte[] query) {
        System.out.println("Hello world!");
        return new byte[0];
    }

}
