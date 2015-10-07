package org.infinispan.atomic.object;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallPersist extends Call implements Externalizable{

   private byte[] bytes;

   @Deprecated
   public CallPersist(){}

   public CallPersist(UUID callerID, byte[] bytes) {
      super(callerID);
      this.bytes = bytes;
   }

   public byte[] getBytes(){
      return bytes;
   }

   @Override
   public String toString() {
      return super.toString()+"-PER";
   }


   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      super.writeExternal(objectOutput);
      objectOutput.writeObject(bytes);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      super.readExternal(objectInput);
      bytes = (byte[]) objectInput.readObject();
   }
}
