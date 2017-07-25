package org.infinispan.creson;

import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public interface ShardedObject {
   ShardedObject getShard();
   UUID getID();
}
