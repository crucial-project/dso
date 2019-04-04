package org.infinispan.creson;

public class ScalableCyclicBarrierTask extends Task{

    public ScalableCyclicBarrierTask(String[] parameters, int calls, int clients) {
        super(parameters, calls, clients);
    }

    @Override
    public void doCall() {
        ((ScalableCyclicBarrier)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return new ScalableCyclicBarrier("barrier-"+id, threads);
    }
}
