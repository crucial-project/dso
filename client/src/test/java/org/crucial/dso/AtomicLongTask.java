package org.crucial.dso;

import org.crucial.dso.client.Client;

import java.util.Random;

public class AtomicLongTask extends Task {

    private Random random;

    public AtomicLongTask(long taskId, String[] parameters, int calls, int threads, int parallelism, Client client) {
        super(taskId, parameters, calls, threads, parallelism, client);
        random = new Random();
    }

    @Override
    public void doCall() {
        AtomicLong integer = (AtomicLong) instances.get(random.nextInt(instances.size()));
        integer.incrementAndGet();
    }

    @Override
    public Object newObject(int id) {
        return client.getAtomicLong("long-test-"+id);
    }

    @Override
    public int checksum(){
        int checksum = 0;
        for (Object o: instances) {
            checksum += ((AtomicLong)o).get();
        }
        return checksum;
    }

}
