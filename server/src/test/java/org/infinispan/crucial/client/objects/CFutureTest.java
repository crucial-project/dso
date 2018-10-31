package org.infinispan.crucial.client.objects;

import org.infinispan.crucial.test.Emulation;
import org.testng.annotations.Test;
import org.infinispan.crucial.CFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Pierre
 */
public class CFutureTest extends Emulation{
    CFuture<Void> future;

    @Override
    protected int numberOfCaches(){
        return 3;
    }

    @Test
    public void test() throws Exception{

        future = new CFuture<>("test");

        ExecutorService service = Executors.newCachedThreadPool();

        List<Future<Void>> toComplete = new ArrayList<>();

        toComplete.add(service.submit(() -> {
            System.out.println("Waiting ... ");
            future.get();
            return null;
        }));

        Thread.sleep(3000);

        toComplete.add(service.submit(() -> {
            future.set(null);
            System.out.println("Triggering ... ");
            return null;
        }));

        for (Future<Void> future : toComplete) {
            future.get();
        }

    }
}
