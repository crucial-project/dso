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

    public CyclicBarrier createBarrier(String name, int parties) { return new CyclicBarrier(name, parties);}

    public Future createFuture(String name){ return new Future(name);}

}

