package org.infinispan.creson.utils;

import com.fasterxml.uuid.NoArgGenerator;
import org.infinispan.creson.object.Reference;

public class Context {

   private NoArgGenerator generator;
   private Reference reference;

   public Context(NoArgGenerator generator, Reference reference) {
      this.generator = generator;
      this.reference = reference;
   }

   public NoArgGenerator getGenerator() {
      return generator;
   }

   public Reference getReference() {
      return reference;
   }

   @Override
   public String toString() {
      return "Context{" +
              "generator=" + generator +
              ", reference=" + reference +
              '}';
   }
}
