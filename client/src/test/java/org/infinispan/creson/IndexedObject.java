package org.infinispan.creson;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;


@Indexed
@Entity
public class IndexedObject implements Serializable {

    @Id
    public int id;

    @Field
    public int value;

    public IndexedObject() {
    }

    public IndexedObject(int id) {
        this.id = id;
    }

    public void setField(int value) {
        this.value = value;
    }

}
