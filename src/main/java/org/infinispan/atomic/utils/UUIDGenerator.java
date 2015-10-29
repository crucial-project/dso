package org.infinispan.atomic.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;

import java.util.Random;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class UUIDGenerator {

   // Thread global

   private static RandomBasedGenerator generator
         = Generators.randomBasedGenerator(new Random(System.currentTimeMillis()));

   public static UUID generate(){
      return generator.generate();
   }

   // Thread local

   private static ThreadLocal<RandomBasedGenerator> tgenerator;

   public static RandomBasedGenerator getThreadLocal() {
      if (tgenerator!=null)
         return tgenerator.get();
      return null;
   }

   public static void setThreadLocal(final RandomBasedGenerator g) {
      tgenerator = new ThreadLocal<RandomBasedGenerator>() {
         @Override
         protected RandomBasedGenerator initialValue() {
            return g;
         }
      };
   }

   public static void unsetThreadLocal() {
      tgenerator = null;
   }

}
