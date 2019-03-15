package org.infinispan.creson.concurrent;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Blob {

    @Id
    public String id;
    public Byte[] content;

    public Blob(){}

    public Blob(String id){
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Byte[] getContent() {
        return content;
    }

    public void setContent(Byte[] content) {
        this.content = content;
    }
}
