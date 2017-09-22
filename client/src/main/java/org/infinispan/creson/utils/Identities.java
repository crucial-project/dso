package org.infinispan.creson.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pierre Sutra
 */
public class Identities {

   private static Map<Long, NoArgGenerator> generator = new ConcurrentHashMap<>();
   static{
      generator.put((long)0,
              Generators.randomBasedGenerator(new Random(System.nanoTime())));
   }

   private static Map<Long, java.util.UUID> threadUUIDs = new ConcurrentHashMap<>();

   public static java.util.UUID getThreadID() {
      long threadID = Thread.currentThread().getId();
      if (!threadUUIDs.containsKey(threadID)) {
         threadUUIDs.putIfAbsent(threadID,
                 generator.get((long) 0).generate());
      }
      return threadUUIDs.get(threadID);
   }

   public static NoArgGenerator getThreadLocal() {
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
