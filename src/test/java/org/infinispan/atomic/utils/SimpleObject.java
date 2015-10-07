package org.infinispan.atomic.utils;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;
import org.infinispan.atomic.ReadOnly;

import java.io.Serializable;

/**
* @author Pierre Sutra
*/
@Distributed
public class SimpleObject implements Serializable {

   @Key
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
