package org.infinispan.creson.utils;

import org.infinispan.creson.Entity;
import org.infinispan.creson.ReadOnly;

import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Entity(key = "id")
public class SimpleShardedObject implements ShardedObject {
   
   public UUID id;
   
   private ShardedObject shard;

   public SimpleShardedObject(){
      id = UUID.randomUUID();
   }
   
   public SimpleShardedObject(ShardedObject shard) {
      id = UUID.randomUUID();
      this.shard = shard;
   }
   
   @ReadOnly
   @Override
   public ShardedObject getShard(){
      return shard;
   }

   @ReadOnly
   @Override
   public String toString(){
      return id.toString();
   }

}
