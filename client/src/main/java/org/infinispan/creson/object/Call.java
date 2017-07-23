package org.infinispan.creson.object;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class Call implements Externalizable {

   private UUID callID;

   @Deprecated
   public Call(){}

   public Call(UUID callID){
      this.callID = callID;
   }

   public UUID getCallID(){
      return callID;
   }

   @Override
   public String toString(){
      return callID.toString();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || !(o instanceof Call)) return false;
      Call call = (Call) o;
      return callID.equals(call.callID);
   }

   @Override
   public int hashCode(){
      return callID.hashCode();
   }

   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeObject(callID);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      callID = (UUID) objectInput.readObject();
   }


}
