package org.crucial.dso;

import org.crucial.dso.client.Client;

public class CountDownLatchTask extends Task{

    public CountDownLatchTask(long taskId, String[] parameters, int calls, int threads, int parallelism, Client client) {
        super(taskId, parameters, calls, threads, parallelism, client);
    }

    @Override
    public void doCall() {
        ((CountDownLatch)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return client.getCountDownLatch("barrier-"+id, parallelism*threads);
    }

}
