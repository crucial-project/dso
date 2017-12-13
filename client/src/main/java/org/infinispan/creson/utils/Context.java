package org.infinispan.creson.utils;

import com.fasterxml.uuid.NoArgGenerator;
import org.infinispan.creson.Factory;
import org.infinispan.creson.object.Reference;

public class Context {

   private NoArgGenerator generator;
   private Reference reference;
   private Factory factory;

   public Context(NoArgGenerator generator, Reference reference, Factory factory) {
      this.generator = generator;
      this.reference = reference;
      this.factory = factory;
   }

   public NoArgGenerator getGenerator() {
      return generator;
   }

   public Reference getReference() {
      return reference;
   }

   public Factory getFactory() {
      return factory;
   }

   @Override
   public String toString() {
      return "Context{" +
              ", reference=" + reference +
              '}';
   }
}
