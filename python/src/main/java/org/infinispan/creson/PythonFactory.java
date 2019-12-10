package org.infinispan.creson;

public class PythonFactory {

    public final static String DEFAULT_SERVER = org.infinispan.creson.client.Interpreter.DEFAULT_SERVER;

    private Factory factory;

    public PythonFactory(){
        factory = Factory.get(DEFAULT_SERVER);
    }

    public PythonFactory(String server){
        factory = Factory.get(server);
    }

    // counter

    public AtomicCounter createCounter(String name){
        return createCounter(name,0);
    }

    public AtomicCounter createCounter(String name, int value){
        return new AtomicCounter(name,value);
    }
}

