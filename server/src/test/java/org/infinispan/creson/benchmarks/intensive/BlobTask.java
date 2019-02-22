package org.infinispan.creson.benchmarks.intensive;

import org.infinispan.creson.benchmarks.Task;

import java.util.List;
import java.util.Random;

public class BlobTask extends Task {

    private Random random;
    private int size;

    public BlobTask(List<Object> objects, String[] parameters, int calls) {
        super(objects, parameters, calls);
        assert parameters.length == 1;
        size = Integer.parseInt(parameters[0]);
        random = new Random();
    }

    @Override
    public void doCall() {
        Byte[] newcontent = new Byte[this.size];
        Blob blob = (Blob) objects.get(random.nextInt(objects.size()));
        blob.setContent(newcontent);
    }
}
