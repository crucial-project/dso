package org.infinispan.atomic;

import org.infinispan.atomic.utils.FakeCacheContainer;
import org.infinispan.commons.api.BasicCacheContainer;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pierre Sutra
 */
@Test(testName = "AtomicObjectFactoryFakeTest", groups = "unit")
public class AtomicObjectFactoryFakeTest extends AtomicObjectFactoryAbstractTest {

   private final List<BasicCacheContainer> containerList = new ArrayList<>();

   @Override
   public BasicCacheContainer container(int i) {
      return containerList.get(i);
   }

   @Override
   public Collection<BasicCacheContainer> containers() {
      return containerList;
   }

   @Override
   public boolean addContainer() {
      FakeCacheContainer container = new FakeCacheContainer();
      containerList.add(container);
      return true;
   }

   @Override
   public boolean deleteContainer() {
      if (containerList.size() == 0)
         return false;
      containerList.remove(containerList.size() - 1);
      return true;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      for (int i = 0; i < getNumberOfManagers(); i++) {
         addContainer();
      }
   }

   protected void clearContent() throws Throwable {}

}
