package org.infinispan.crucial.utils;

import com.fasterxml.uuid.NoArgGenerator;
import org.infinispan.crucial.Factory;

import java.util.UUID;

public class Context {

   private final UUID callerID;
   private final NoArgGenerator generator;
   private final Factory factory;

   public Context(UUID callerID, NoArgGenerator generator, Factory factory) {
      this.callerID = callerID;
      this.generator = generator;
      this.factory = factory;

   }

   public Factory getFactory() {
      return factory;
   }

   public UUID getCallerID(){ return callerID;}

   public NoArgGenerator getGenerator() {
      return generator;
   }

   @Override
   public String toString() {
      return "Context{" + callerID + ", generator="+generator+'}';
   }

}
