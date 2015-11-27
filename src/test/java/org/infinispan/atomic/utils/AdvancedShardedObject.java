package org.infinispan.atomic.utils;

import org.infinispan.atomic.DistClass;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@DistClass(key="uuid")
public class AdvancedShardedObject implements ShardedObject{

   public UUID uuid;

   private AdvancedShardedObject shard;
   private boolean value;

   // @Distribute
   private List<AdvancedShardedObject> list;

   public AdvancedShardedObject(){
      this.uuid = UUID.randomUUID();
   }

   public AdvancedShardedObject(AdvancedShardedObject shard){
      this.uuid = UUID.randomUUID();
      this.shard = shard;
      this.value = false;
      list = new ArrayList<>();
      list.add(shard);
   }

   @Override
   public ShardedObject getShard() {
      return shard;
   }

   public List<AdvancedShardedObject> getAsList() {
      return list;
   }

   public boolean flipValue(){
      if (shard!=null)
         return shard.flipValue();
      value = !value;
      return value;
   }

   public boolean setShard(AdvancedShardedObject shard) {
      this.shard = shard;
      return shard.flipValue();
   }


}
