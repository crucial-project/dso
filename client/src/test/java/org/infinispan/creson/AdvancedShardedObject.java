package org.infinispan.creson;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Entity
public class AdvancedShardedObject implements ShardedObject{

   @Id private UUID uuid;
   @Shared private List<AdvancedShardedObject> shards = new ArrayList<>();
   private boolean value;

   @Deprecated
   public AdvancedShardedObject(){}

   public AdvancedShardedObject(UUID uuid){
      this.uuid = uuid;
      this.value = false;
   }

   @Override
   public ShardedObject getShard() {
      return shards.get(0);
   }

   @Override
   public UUID getID() {
      return uuid;
   }

   public List<AdvancedShardedObject> getList() {
      return shards;
   }

   public boolean flipValue(){
      if (getShard()!=null) {
         getShard().getID();
         return shards.get(0).flipValue();
      }
      value = !value;
      return value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      AdvancedShardedObject that = (AdvancedShardedObject) o;
      return uuid.equals(that.uuid);
   }

   @Override
   public int hashCode() {
      return uuid.hashCode();
   }

   @Override
   public String toString() {
      return "AdvancedShardedObject#"+uuid.toString();
   }

}
