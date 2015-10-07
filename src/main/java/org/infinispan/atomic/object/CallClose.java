package org.infinispan.atomic.object;

import java.util.UUID;

/**
  *
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallClose extends Call {

   @Deprecated
   public CallClose(){}

   public CallClose(UUID callerID) {
      super(callerID);
   }

   @Override
   public String toString() {
      return super.toString()+"-CLOSE";
   }

}
