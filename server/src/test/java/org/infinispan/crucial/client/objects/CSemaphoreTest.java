package org.infinispan.crucial.client.objects;

import org.infinispan.crucial.CFuture;
import org.infinispan.crucial.CSemaphore;
import org.infinispan.crucial.client.CrucialClient;
import org.infinispan.crucial.test.Emulation;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Pierre
 */
public class CSemaphoreTest extends Emulation{
    CSemaphore semaphore;

    @Override
    protected int numberOfCaches(){
        return 2;
    }

    @Test
    public void test() throws Exception{
        CrucialClient cc = CrucialClient.getClient();

        semaphore = cc.getSemaphore("sem", 0);

        ExecutorService service = Executors.newCachedThreadPool();

        List<Future<Void>> toComplete = new ArrayList<>();

        toComplete.add(service.submit(() -> {
            System.out.println("Acquiring ... ");
            semaphore.acquire();
            return null;
        }));
        toComplete.add(service.submit(() -> {
            System.out.println("Acquiring ... ");
            semaphore.acquire();
            return null;
        }));

        Thread.sleep(3000);

        toComplete.add(service.submit(() -> {
            System.out.println("Releasing ... ");
            semaphore.release();
            return null;
        }));

        toComplete.add(service.submit(() -> {
            System.out.println("Releasing ... ");
            semaphore.release();
            return null;
        }));

        for (Future<Void> future : toComplete) {
            future.get();
        }

    }
}
