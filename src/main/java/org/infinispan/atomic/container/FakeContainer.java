package org.infinispan.atomic.container;

import org.infinispan.atomic.object.Reference;
import org.infinispan.atomic.object.Utils;
import org.infinispan.commons.api.BasicCache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Pierre Sutra
 */
public class FakeContainer extends AbstractContainer {
   
   private static ConcurrentMap<Reference,Object> objects = new ConcurrentHashMap<>();

   public FakeContainer(BasicCache cache, Reference reference, boolean readOptimization, boolean forceNew,
         List<String> methods, Object... initArgs) {
      super(cache, reference, readOptimization, forceNew, methods, initArgs);
      
      try {
         Object o = Utils.initObject(reference.getClazz(), initArgs);
         objects.putIfAbsent(reference,o);
         proxy = objects.get(reference);
      } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
         e.printStackTrace(); 
      } 
      
   }

   @Override 
   public void open() throws InterruptedException, ExecutionException, TimeoutException, IOException {
      // nothing to do
   }

   @Override 
   public void close()
         throws InterruptedException, ExecutionException, TimeoutException, IOException {
      // nothing to do
   }

   @Override 
   public UUID listenerID() {
      return UUID.randomUUID();
   }

}
