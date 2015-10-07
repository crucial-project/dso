package org.infinispan.atomic.object;

import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallRetrieve extends Call {

   @Deprecated
   public CallRetrieve(){}
   
   public CallRetrieve(UUID callerID) {
      super(callerID);
   }
   
   @Override
   public String toString() {
      return super.toString()+"-RET";
   }

}
