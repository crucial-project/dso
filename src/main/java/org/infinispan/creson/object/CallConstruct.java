package org.infinispan.creson.object;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class CallConstruct extends Call{

   private boolean forceNew;
   private boolean readOptimization;
   private Object[] initArgs;
   
   @Deprecated
   public CallConstruct(){}

   public CallConstruct(UUID callID, boolean forceNew, Object[] initargs, boolean readOptimization) {
      super(callID);
      this.forceNew = forceNew;
      this.initArgs = initargs;
      this.readOptimization = readOptimization;
   }

   @Override
   public String toString() {
      return super.toString()+"-CONS";
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
