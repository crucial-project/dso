package org.infinispan.creson;

public class ScalableCyclicBarrierTask extends Task{

    public ScalableCyclicBarrierTask(String[] parameters, int calls, int threads, int parallelism) {
        super(parameters, calls, threads, parallelism);
    }

    @Override
    public void doCall() {
        ((ScalableCyclicBarrier)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return new ScalableCyclicBarrier("barrier-"+id, parallelism*threads);
    }
}
