package org.infinispan.atomic.utils;

import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.object.Reference;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Pierre Sutra
 */
public class AOFUtils {

   public static AtomicObjectFactory createAOF(BasicCache cache, boolean isThreadLocal) {
      return null;
   }

   public static AtomicObjectFactory createAOF(String server) {
      int port = Integer.valueOf(server.split(":")[1]);
      String host = server.split(":")[0];
      org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
            = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
      cb.tcpNoDelay(true)
            .addServer()
            .host(host)
            .port(port);
      RemoteCacheManager manager= new RemoteCacheManager(cb.build());
      return AtomicObjectFactory.forCache(manager.getCache());
   }

   public static Object unreference(Reference reference, AtomicObjectFactory factory) {
      return factory.getInstanceOf(reference);
   }

   public static Object unreference(Object arg, BasicCache cache) {
      return unreference(Collections.singleton(arg).toArray(),cache)[0];
   }

   public static Object[] unreference(Object[] args, BasicCache cache) {
      List<Object> ret = new ArrayList<>(args.length);
      for(Object arg : args) {
         if (arg instanceof Reference) {
            ret.add(unreference((Reference)arg, AtomicObjectFactory.forCache(cache)));
         } else {
            if (arg instanceof List) {
               List list = new ArrayList(((List) arg).size());
               for(Object item: (List)arg) {
                  list.add(unreference(Collections.singleton(item).toArray(),cache)[0]);
               }
               ((List) arg).clear();
               ((List)arg).addAll(list);
            }
            ret.add(arg);
         }
      }
      return ret.toArray();
   }


}
