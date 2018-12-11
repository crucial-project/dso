package org.infinispan.crucial.client.objects;


import org.infinispan.crucial.CCyclicBarrier;
import org.infinispan.crucial.test.Emulation;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ...
 * <p>
 * Date: 2018-05-21
 *
 * @author Pierre
 */
public class CyclicBarrierTest extends Emulation{

    CCyclicBarrier barrier;

    @Override
    protected int numberOfCaches(){
        return 3;
    }

    @Test
    public void test() throws Exception{
        final int ITERATIONS = 10;
        final int THREADS = 10;

        barrier = new CCyclicBarrier("test", THREADS);
        ExecutorService service = Executors.newFixedThreadPool(THREADS);
        List<Future<Void>> toComplete = new ArrayList<>();

        for (int j = 0; j < ITERATIONS; j++) {
        	System.out.println("\n== New iteration: " + j + " ==");

            for (int i = 0; i < THREADS; i++) {
                int finalI = i;
                toComplete.add(service.submit(() ->
                {
                    System.out.println("Waiting ... (" + finalI + ")");
                    barrier.await();
                    System.out.println("Ready ! (" + finalI + ")");
                    return null;
                }));
            }

            for (Future<Void> future : toComplete) {
                future.get();
            }
        }
    }
}