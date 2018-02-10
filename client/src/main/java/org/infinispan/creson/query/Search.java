package org.infinispan.creson.query;


import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;

public class Search {

    private Search() {
    }

    public static QueryFactory getQueryFactory(RemoteCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("cache parameter cannot be null");
        }
        return new QueryFactory((RemoteCacheImpl) cache);
    }

}
