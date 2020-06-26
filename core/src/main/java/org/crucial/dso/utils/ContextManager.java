package org.crucial.dso.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import org.crucial.dso.Factory;

import java.util.Random;

/**
 * @author Pierre Sutra
 */
public class ContextManager {

   private static NoArgGenerator generator
           = Generators.randomBasedGenerator(new Random(System.nanoTime()));

   public static void seedGenerator(long seed){
      generator = Generators.randomBasedGenerator(new Random(seed));
   }

   private static ThreadLocal<Context> context =
           ThreadLocal.withInitial(() -> new Context(
                   generator.generate(),
                   generator,
                   Factory.getSingleton()));

   public static Context get(){
      return context.get();
   }

   public static void set(Context c) {
      context.set(c);
   }

}
