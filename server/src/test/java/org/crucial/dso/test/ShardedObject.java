package org.crucial.dso.test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Entity
public class ShardedObject implements Serializable {

    @Id public String id;
    private boolean value;
    private ShardedObject shard;

    public ShardedObject() {}

    public ShardedObject(String id) {
        this.id = id;
    }

    public ShardedObject(String id, ShardedObject shard) {
        this.id = id;
        this.shard = shard;
    }

    public void addShard(ShardedObject other) {
        this.shard = other;
    }

    public ShardedObject getShard() {
        return shard;
    }

    public String getID() {
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
