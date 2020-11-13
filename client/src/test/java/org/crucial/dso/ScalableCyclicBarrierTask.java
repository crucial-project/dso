package org.crucial.dso;

import org.crucial.dso.client.Client;

public class ScalableCyclicBarrierTask extends Task{

    public ScalableCyclicBarrierTask(long taskId, String[] parameters, int calls, int threads, int parallelism, Client client) {
        super(taskId, parameters, calls, threads, parallelism, client);
    }

    @Override
    public void doCall() {
        ((ScalableCyclicBarrier)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return client.getScalableCyclicBarrier("barrier-"+id, parallelism*threads);
    }
}
