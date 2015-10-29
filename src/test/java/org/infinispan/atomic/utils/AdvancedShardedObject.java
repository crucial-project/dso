package org.infinispan.atomic.utils;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;

import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Distributed
public class AdvancedShardedObject implements ShardedObject{

   @Key
   public UUID uuid;
   private AdvancedShardedObject shard;
   boolean value;

   public AdvancedShardedObject(){
      this(null);
   }

   public AdvancedShardedObject(AdvancedShardedObject shard){
      this.uuid = UUID.randomUUID();
      this.shard = shard;
      this.value = false;
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
