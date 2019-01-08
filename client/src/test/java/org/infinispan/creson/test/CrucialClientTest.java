package org.infinispan.creson.test;

import org.infinispan.creson.CAtomicInt;
import org.infinispan.creson.client.CrucialClient;
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
