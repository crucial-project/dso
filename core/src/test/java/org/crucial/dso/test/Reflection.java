package org.crucial.dso.test;

import org.testng.annotations.Test;

import java.util.HashSet;

public class Reflection {

    @Test
    public void baseClasses() throws NoSuchMethodException {
        Class[] params = {Object.class};
        assert org.crucial.dso.utils.Reflection.isMethodSupported(HashSet.class,HashSet.class.getMethod("add",params));
        assert !org.crucial.dso.utils.Reflection.isMethodSupported(HashSet.class,HashSet.class.getMethod("wait",(Class<?>[])null));
    }

}
