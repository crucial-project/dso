package org.infinispan.atomic.utils;

import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.client.hotrod.RemoteCacheManager;

/**
 * @author Pierre Sutra
 */
public class AOFUtils {

   public static void createAOF(String server) {
      int port = Integer.valueOf(server.split(":")[1]);
      String host = server.split(":")[0];
      org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
            = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
      cb.tcpNoDelay(true)
            .addServer()
            .host(host)
            .port(port);
      RemoteCacheManager manager= new RemoteCacheManager(cb.build());
      AtomicObjectFactory.forCache(manager.getCache());
   }
   
}
