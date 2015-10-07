package org.infinispan.atomic.object;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/**
 *
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallOpen extends Call{

   private boolean forceNew;
   private boolean readOptimization;
   private Object[] initArgs;
   
   @Deprecated
   public CallOpen(){}

   public CallOpen(UUID callerID, boolean forceNew, Object[] initargs, boolean readOptimization) {
      super(callerID);
      this.forceNew = forceNew;
      this.initArgs = initargs;
      this.readOptimization = readOptimization;
   }

   @Override
   public String toString() {
      return super.toString()+"-OPEN";
   }

   public boolean getForceNew() {
      return forceNew;
   }
   
   public boolean getReadOptimization(){return readOptimization;}
   
   public Object[] getInitArgs(){
      return initArgs;
   }

   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      super.writeExternal(objectOutput);
      objectOutput.writeBoolean(forceNew);
      objectOutput.writeObject(initArgs);
      objectOutput.writeBoolean(readOptimization);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      super.readExternal(objectInput);
      forceNew = objectInput.readBoolean();
      initArgs = (Object[]) objectInput.readObject();
      readOptimization = objectInput.readBoolean();
   }

}
