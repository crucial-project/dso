package org.infinispan.creson.object;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class CallInvoke extends Call implements Externalizable{

   public UUID callerID;
   public String method;
   public Object[] arguments;

   @Deprecated
   public CallInvoke(){}

   public CallInvoke(UUID callID, UUID caller, String m, Object[] args) {
      super(callID);
      callerID = caller;
      method = m;
      arguments = args;

   }

   public UUID getCallerID(){
      return callerID;
   }

   @Override
   public String toString(){
      String args = " ";
      for(Object a : arguments){
         args+=(a==null?"null":a.toString())+" ";
      }
      return super.toString()+"-INV-"+method+ "()";
   }


   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      super.writeExternal(objectOutput);
      objectOutput.writeObject(callerID);
      objectOutput.writeObject(method);
      objectOutput.writeObject(arguments);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      super.readExternal(objectInput);
      callerID = (UUID) objectInput.readObject();
      method = (String) objectInput.readObject();
      arguments = (Object[]) objectInput.readObject();
   }

}
