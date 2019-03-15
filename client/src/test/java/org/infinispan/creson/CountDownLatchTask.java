package org.infinispan.creson;

public class CountDownLatchTask extends Task{

    public CountDownLatchTask(String[] parameters, int calls, int clients) {
        super(parameters, calls, clients);
    }

    @Override
    public void doCall() {
        ((CountDownLatch)this.instances.get(0)).await();
    }

    @Override
    public Object newObject(int id) {
        return new CountDownLatch("barrier-"+id,clients);
    }

}
