package org.infinispan.creson;

public class CountDownLatchTask extends Task{

    public CountDownLatchTask(String[] parameters, int calls, int threads, int parallelism) {
        super(parameters, calls, threads, parallelism);
    }

    @Override
    public void doCall() {
        ((CountDownLatch)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return new CountDownLatch("barrier-"+id, threads);
    }

}
