package org.infinispan.crucial.test;

import org.infinispan.crucial.CAtomicInt;
import org.infinispan.crucial.client.CrucialClient;
import org.testng.annotations.Test;

/**
 *
 * @author Daniel
 */
public class CrucialClientTest{

    @Test
    public void testCreateClient(){
        CrucialClient cc = CrucialClient.getClient("localhost:11222");

        CAtomicInt ai = cc.getAtomicInt("count");
        System.out.println(ai.incrementAndGet());
        ai.printValue();
    }

}
