package org.infinispan.creson;

public class CyclicBarrierTask extends Task{

    public CyclicBarrierTask(long taskId, String[] parameters, int calls, int threads, int parallelism) {
        super(taskId, parameters, calls, threads, parallelism);
    }

    @Override
    public void doCall() {
        ((CyclicBarrier)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return new CyclicBarrier("barrier-"+id, parallelism*threads);
    }
}
