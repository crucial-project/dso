package org.infinispan.atomic.utils;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;
import org.infinispan.atomic.ReadOnly;

import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Distributed
public class SimpleShardedObject implements SimpleShardedObjectInterface{
   
   @Key
   public UUID id;
   
   public SimpleShardedObjectInterface shard;

   public SimpleShardedObject(){
      id = UUID.randomUUID();
   }
   
   public SimpleShardedObject(SimpleShardedObjectInterface shard) {
      id = UUID.randomUUID();
      this.shard = shard;
   }
   
   @ReadOnly
   @Override
   public SimpleShardedObjectInterface getShard(){
      return shard;
   }

   @ReadOnly
   @Override
   public String toString(){
      return id.toString();
   }

}
