package org.infinispan.creson.test;

import javassist.util.proxy.Proxy;
import org.infinispan.creson.Factory;
import org.infinispan.creson.Shared;
import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre Sutra
 */

@Test(testName = "TestAspectJ")
public class TestAspectJ {

    static{
        Factory.forCache(new FakeCache());
    }

    @Shared private List list;

    @Test
    public void sharedAnnotation() {
        list = new ArrayList();
        assert list instanceof Proxy;
    }

    @Test
    public void entityAnnotation(){
        A a = new A();
        assert a instanceof Proxy;
    }

    @Entity
    private static class A{

        private A(){}

        @Id
        private String f = "a";

    }

}
