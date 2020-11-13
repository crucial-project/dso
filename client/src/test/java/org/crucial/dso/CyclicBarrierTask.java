package org.crucial.dso;

import org.crucial.dso.client.Client;

public class CyclicBarrierTask extends Task{

    public CyclicBarrierTask(long taskId, String[] parameters, int calls, int threads, int parallelism, Client client) {
        super(taskId, parameters, calls, threads, parallelism, client);
    }

    @Override
    public void doCall() {
        ((CyclicBarrier)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return client.getCyclicBarrier("barrier-"+id, parallelism*threads);
    }
}
