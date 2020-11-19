package org.crucial.dso.test;

import javassist.util.proxy.Proxy;
import org.crucial.dso.Shared;
import org.crucial.dso.Factory;
import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre Sutra
 */

@Test(testName = "BaseAspectJ")
public class BaseAspectJ {

    static{
        Factory.forCache(new FakeCache());
    }

    @Shared
    private List list;

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
    public static class A{

        public A(){}

        @Id
        private String f = "a";

    }

}
