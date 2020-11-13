package org.crucial.dso;

import org.crucial.dso.client.Client;

public class PythonFactory {

    private Client client;

    public PythonFactory(String server){
        client = Client.getClient(server);
    }

    public AtomicCounter createCounter(String name, int value){
        return client.getAtomicCounter(name,value);
    }

    public AtomicMap createMap(String name){
        return client.getAtomicMap(name);
    }

    public <T> AtomicMatrix<T> createMatrix(String name, Class<T> clazz, int n, int m) {
        return client.getAtomicMatrix(name, clazz, n, m);
    }

}

