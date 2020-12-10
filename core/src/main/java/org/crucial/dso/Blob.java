package org.crucial.dso;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class Blob implements Serializable {

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
