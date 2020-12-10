package org.crucial.dso.test;

import javassist.util.proxy.Proxy;
import org.crucial.dso.Shared;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Test(testName = "RemoteAspectJ")
public class RemoteAspectJ extends RemoteTest{

    @Test(groups = {"aspectj"})
    public void base() throws Exception {

        // 1 - constructor
        SimpleObject object = new SimpleObject("aspectj");
        String field = object.getField();
        assert field.equals("aspectj");

        // 2 - constructor w. arguments
        SimpleObject object1 = new SimpleObject("aspectj2");
        assert object1.getField().equals("aspectj2");

        // 3 - equals()
        ShardedObject object2 = new ShardedObject("aspectj3");
        assert object2.equals(object2);

    }

    @Shared
    List<SimpleObject> l1;

    @Test(groups = {"aspectj"})
    public void annotation() throws Exception{
        l1 = new ArrayList<>();
        SimpleObject object1 = new SimpleObject();
        l1.add(object1);
        assert l1 instanceof Proxy;
        assert l1.size() == 1;
        l1.remove(0);
        assert l1.size() == 1;
    }


}
