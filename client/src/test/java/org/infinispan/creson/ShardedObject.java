package org.infinispan.creson;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Entity
public class ShardedObject {

    @Id public UUID id;
    private boolean value;
    private ShardedObject shard;

    public ShardedObject() {
        id = UUID.randomUUID();
    }

    public ShardedObject(ShardedObject shard) {
        id = UUID.randomUUID();
        this.shard = shard;
    }

    public ShardedObject getShard() {
        return shard;
    }

    public UUID getID() {
        return id;
    }

    public boolean flipValue(){
        if (getShard()!=null) {
            getShard().getID();
            return getShard().flipValue();
        }
        value = !value;
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShardedObject that = (ShardedObject) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ShardedObject#"+id.toString();
    }


}
