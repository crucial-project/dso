package org.infinispan.atomic.object;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class CallPersist extends Call {

   private byte[] bytes;
   private int openCallsCounter;

   @Deprecated
   public CallPersist(){}

   public CallPersist(UUID callerID, UUID callID, byte[] bytes, int openCallsCounter) {
      super(callerID, callID);
      this.bytes = bytes;
      this.openCallsCounter = openCallsCounter;
   }

   public byte[] getBytes(){
      return bytes;
   }

   public int getOpenCallsCounter() {
      return openCallsCounter;
   }

   @Override
   public String toString() {
      return super.toString()+"-PER";
   }


   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      super.writeExternal(objectOutput);
      objectOutput.writeObject(bytes);
      objectOutput.writeInt(openCallsCounter);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      super.readExternal(objectInput);
      bytes = (byte[]) objectInput.readObject();
      openCallsCounter = objectInput.readInt();
   }

}
