package org.infinispan.creson;

import java.util.Random;

public class BlobTask extends Task {

    private Random random;
    private int size;

    public BlobTask(String[] parameters, int calls, int threads, int parallelism) {
        super(parameters, calls, threads, parallelism);
        assert parameters != null && parameters.length == 1;
        size = Integer.parseInt(parameters[0]);
        random = new Random();
    }

    @Override
    public void doCall() {
        Byte[] newcontent = new Byte[this.size];
        Blob blob = (Blob) instances.get(random.nextInt(instances.size()));
        blob.setContent(newcontent);
    }

    @Override
    public Object newObject(int id) {
        return new Blob("blob-test-"+id);
    }
}
