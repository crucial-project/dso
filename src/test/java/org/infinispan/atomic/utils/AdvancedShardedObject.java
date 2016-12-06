package org.infinispan.atomic.utils;

import org.infinispan.atomic.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Entity(key="uuid")
public class AdvancedShardedObject implements ShardedObject{

   @Entity(key = "list")
   public static List<AdvancedShardedObject> list = new ArrayList<>();
   public static List<AdvancedShardedObject> getList() {
      return list;
   }

   public UUID uuid;
   private AdvancedShardedObject shard;
   private boolean value;

   @Deprecated
   public AdvancedShardedObject(){}

   public AdvancedShardedObject(UUID uuid){
      this(uuid,null);
   }

   public AdvancedShardedObject(UUID uuid, AdvancedShardedObject shard){
      this.uuid = uuid;
      this.shard = shard;
      this.value = false;
   }

   @Override
   public ShardedObject getShard() {
      return shard;
   }

   public boolean addSelf(){
      return list.add(this);
   }

   public AdvancedShardedObject getSelf(){
      return this;
   }

   public boolean flipValue(){
      if (shard!=null) {
         shard.getId();  // for the sake of adding another call
         return shard.flipValue();
      }
      value = !value;
      return value;
   }

   public String toString() {
      return "AdvancedShardedObject#"+uuid.toString();
   }

   @Override
   public boolean equals(Object o) {

      if (this == o)
         return true;

      if (o == null || !(o instanceof AdvancedShardedObject))
         return false;

      AdvancedShardedObject object = (AdvancedShardedObject) o;

      return uuid.equals(object.getId());

   }

   public UUID getId() {
      return uuid;
   }
}
