package org.infinispan.crucial;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
* @author Pierre Sutra
*/
@Entity
public class SimpleObject {

   @Id public String field;

   public SimpleObject(){
      field = "test";
   }

   public SimpleObject(String f){
      field = f;
   }

   public String getField(){ return field;}

   public void setField(String f){
      field = f;
   }

   public String toString(){
      return "SimpleObject["+field+"]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      SimpleObject that = (SimpleObject) o;

      return field.equals(that.field);
   }

   @Override
   public int hashCode() {
      return field.hashCode();
   }
}
