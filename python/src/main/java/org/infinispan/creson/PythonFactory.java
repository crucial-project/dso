package org.infinispan.creson;

public class PythonFactory {

    private Factory factory;

    public PythonFactory(String server){
        factory = Factory.get(server);
    }

    public AtomicCounter createCounter(String name, int value){
        return new AtomicCounter(name,value);
    }

    public AtomicMap createMap(String name){ return new AtomicMap(name);}

    public <T> AtomicMatrix<T> createMatrix(String name, Class<T> clazz, int n, int m) { return new AtomicMatrix(name, clazz, n, m);}

}

