package org.infinispan.creson.test;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.creson.Factory;
import org.infinispan.creson.IndexedObject;
import org.infinispan.creson.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;

public class QueryTest extends Emulation{

    RemoteCache cache;

    @BeforeMethod
    public void setUp() throws Exception {
        cache = remoteCacheManagers.get(0).getCache(CRESON_CACHE_NAME);
        Factory.forCache(cache);
    }

    @Test
    public void shouldQueryObjects() throws IOException {

        IndexedObject obj1 = new IndexedObject(1);
        IndexedObject obj2 = new IndexedObject(2);
        IndexedObject obj3 = new IndexedObject(3);
        obj1.setField(8);
        obj2.setField(7);
        obj3.setField(7);

        QueryFactory qf = Search.getQueryFactory(cache);
        Query q = qf.create("from org.infinispan.creson.IndexedObject o where o.value = 7");
        assert (q.list().size() == 2);
    }

}
