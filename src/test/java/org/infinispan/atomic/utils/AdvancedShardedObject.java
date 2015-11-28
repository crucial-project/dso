package org.infinispan.atomic.utils;

import org.infinispan.atomic.Distribute;
import org.infinispan.atomic.Distributed;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Distributed(key="uuid")
public class AdvancedShardedObject implements ShardedObject{

   @Distribute(key = "list")
   public static List<AdvancedShardedObject> list = new ArrayList<>();
   public static List<AdvancedShardedObject> getList() {
      return list;
   }

   public UUID uuid;
   private AdvancedShardedObject shard;
   private boolean value;

   public AdvancedShardedObject(){
      this.uuid = UUID.randomUUID();
   }

   public AdvancedShardedObject(AdvancedShardedObject shard){
      this.uuid = UUID.randomUUID();
      this.shard = shard;
      this.value = false;
      list.add(shard);
   }

   @Override
   public ShardedObject getShard() {
      return shard;
   }

   public boolean flipValue(){
      if (shard!=null)
         return shard.flipValue();
      value = !value;
      return value;
   }

}
