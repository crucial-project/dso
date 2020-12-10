package org.crucial.dso.test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

/**
* @author Pierre Sutra
*/
@Entity
public class SimpleObject implements Serializable {

   @Id public String field;
   public int count;

   public SimpleObject(){
      field = "test";
      count = 0;
   }

   public SimpleObject(String f){
      field = f;
   }

   public String getField(){ return field;}

   public int getCount(){ return count;}

   public void setField(String f){
      field = f;
      count ++;
   }

   public String toString(){
      return "SimpleObject["+field+","+count+"]";
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
