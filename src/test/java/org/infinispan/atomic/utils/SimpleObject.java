package org.infinispan.atomic.utils;

import org.infinispan.atomic.DistClass;
import org.infinispan.atomic.ReadOnly;

/**
* @author Pierre Sutra
*/
@DistClass(key="field")
public class SimpleObject {

   public String field;

   public SimpleObject(){
      field = "test";
   }

   public SimpleObject(String f){
      field = f;
   }

   @ReadOnly
   public String getField(){ return field;}

   public void setField(String f){
      field = f;
   }

   @ReadOnly
   public String toString(){
      return "SimpleObject["+field+"]";
   }

}
