package org.infinispan.atomic.utils;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;
import org.infinispan.atomic.ReadOnly;

import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Distributed
public class SimpleShardedObject{
   
   @Key
   public UUID id;
   
   public SimpleShardedObject shard;

   public SimpleShardedObject(){
      id = UUID.randomUUID();
   }
   
   public SimpleShardedObject(SimpleShardedObject shard) {
      id = UUID.randomUUID();
      this.shard = shard;
   }
   
   @ReadOnly
   public SimpleShardedObject getShard(){
      return shard;
   }

}
