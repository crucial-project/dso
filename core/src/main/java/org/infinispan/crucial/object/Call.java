package org.infinispan.crucial.object;

import org.infinispan.crucial.utils.ContextManager;

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
   private Reference reference;

   @Deprecated
   public Call(){}

   public Call(Reference reference, java.util.UUID callID){
      this(reference, ContextManager.get().getCallerID(),callID);
   }

   public Call(Reference reference, Call call) {
      this(reference, call.callerID,call.callID);
   }

   private Call(Reference reference, java.util.UUID callerID, java.util.UUID callID){
      this.callerID = callerID;
      this.callID = callID;
      this.reference = reference;
   }

   public java.util.UUID getCallerID(){
      return callerID;
   }

   public java.util.UUID getCallID(){
      return callID;
   }

   public Reference getReference(){
      return reference;
   }

   @Override
   public String toString(){
      return reference.toString();
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
      objectOutput.writeObject(reference);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      callerID = (java.util.UUID) objectInput.readObject();
      callID = (java.util.UUID)  objectInput.readObject();
      reference = (Reference) objectInput.readObject();
   }


}
