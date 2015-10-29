package org.infinispan.atomic.utils;

/**
 * @author Pierre Sutra
 */
public interface ShardedObject {
   ShardedObject getShard();
}
