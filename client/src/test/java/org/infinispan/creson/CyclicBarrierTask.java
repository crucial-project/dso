package org.infinispan.creson;

public class CyclicBarrierTask extends Task{

    public CyclicBarrierTask(String[] parameters, int calls, int clients) {
        super(parameters, calls, clients);
    }

    @Override
    public void doCall() {
        ((CyclicBarrier)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return new CyclicBarrier("barrier-"+id, clients);
    }
}
