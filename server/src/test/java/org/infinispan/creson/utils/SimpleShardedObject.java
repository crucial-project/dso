package org.infinispan.creson.utils;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Entity
public class SimpleShardedObject implements ShardedObject {

    @Id
    public UUID id;

    private ShardedObject shard;

    public SimpleShardedObject() {
        id = UUID.randomUUID();
    }

    public SimpleShardedObject(ShardedObject shard) {
        id = UUID.randomUUID();
        this.shard = shard;
    }

    @Override
    public ShardedObject getShard() {
        return shard;
    }

    @Override
    public String toString() {
        return id.toString();
    }

}
