package org.infinispan.creson.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.infinispan.creson.object.Reference;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pierre Sutra
 */
public class ContextManager {

   private static Map<java.util.UUID, Context> contexts = new ConcurrentHashMap<>();

   // a thread ID is unique in the distributed system
   private static Map<Long, java.util.UUID> threadUUIDs = new ConcurrentHashMap<>();
   private static NoArgGenerator generator = Generators.randomBasedGenerator(new Random(System.nanoTime()));

   public static java.util.UUID getThreadID() {
      long threadID = Thread.currentThread().getId();
      if (!threadUUIDs.containsKey(threadID)) {
         threadUUIDs.putIfAbsent(threadID,generator.generate());
      }
      return threadUUIDs.get(threadID);
   }

   public static Context getContext() {
      return contexts.get(getThreadID());
   }

   public static void setContext(final RandomBasedGenerator g, final Reference r) {
      contexts.put(getThreadID(), new Context(g,r));
   }

   public static void unsetContext() {
      contexts.remove(getThreadID());
   }

}
