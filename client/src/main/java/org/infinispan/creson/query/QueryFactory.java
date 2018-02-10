package org.infinispan.creson.query;

import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.impl.BaseQueryFactory;

public class QueryFactory extends BaseQueryFactory {

    private final long startOffset = -1L;
    private final int maxResults = -1;
    private RemoteCacheImpl cache;

    public QueryFactory(RemoteCacheImpl c) {
        this.cache = c;
    }

    @Override
    public Query create(String jpqlString) {
        return new RemoteQuery(cache, jpqlString, startOffset, maxResults);
    }

    @Override
    public QueryBuilder from(Class<?> entityType) {
        return null;
    }

    @Override
    public QueryBuilder from(String entityType) {
        return null;
    }

}
