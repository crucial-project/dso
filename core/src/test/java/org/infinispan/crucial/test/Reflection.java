package org.infinispan.crucial.test;

import org.testng.annotations.Test;

import java.util.HashSet;

import static org.infinispan.crucial.utils.Reflection.isMethodSupported;

public class Reflection {

    @Test
    public void baseClasses() throws NoSuchMethodException {
        Class[] params = {Object.class};
        assert isMethodSupported(HashSet.class,HashSet.class.getMethod("add",params));
        assert !isMethodSupported(HashSet.class,HashSet.class.getMethod("wait",(Class<?>[])null));
    }

}
