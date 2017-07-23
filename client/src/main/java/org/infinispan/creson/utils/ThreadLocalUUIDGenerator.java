package org.infinispan.creson.utils;

import com.fasterxml.uuid.impl.RandomBasedGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pierre Sutra
 */
public class ThreadLocalUUIDGenerator {

   private static Map<Long, RandomBasedGenerator> generator = new ConcurrentHashMap<>();

   public static RandomBasedGenerator getThreadLocal() {
      if (generator.get(Thread.currentThread().getId())==null)
         return null;
      return generator.get(Thread.currentThread().getId());
   }

   public static void setThreadLocal(final RandomBasedGenerator g) {
      generator.put(Thread.currentThread().getId(),g);
   }

   public static void unsetThreadLocal() {
      generator.remove(Thread.currentThread().getId());
   }

}
