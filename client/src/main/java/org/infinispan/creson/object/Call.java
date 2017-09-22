package org.infinispan.creson.object;

import org.infinispan.creson.utils.Identities;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Pierre Sutra
 */
public class Call implements Externalizable {

   private java.util.UUID callerID;
   private java.util.UUID callID;

   @Deprecated
   public Call(){}

   public Call(java.util.UUID callID){
      this(Identities.getThreadID(),callID);
   }

   public Call(Call call) {
      this(call.callerID,call.callID);
   }

   private Call(java.util.UUID callerID, java.util.UUID callID){
      this.callerID = callerID;
      this.callID = callID;
   }




   public java.util.UUID getCallerID(){
      return callerID;
   }

   public java.util.UUID getCallID(){
      return callID;
   }

   @Override
   public String toString(){
      return callerID.toString()+":"+callID.toString();
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
      objectOutput.writeObject(callerID);
      objectOutput.writeObject(callID);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      callerID = (java.util.UUID) objectInput.readObject();
      callID = (java.util.UUID) objectInput.readObject();
   }


}
