package org.infinispan.crucial;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Pierre Sutra
 */
public aspect Marshalling{

    interface Marshallable extends Externalizable{
    }

    public void Marshallable.writeExternal(ObjectOutput objectOutput) throws IOException{
        try {
            for (Field field : this.getClass().getFields()) {
                if (! Modifier.isTransient(field.getModifiers()) &&
                        ! Modifier.isStatic(field.getModifiers())) {
                    Object object = field.get(this);
                    objectOutput.writeObject(object);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void Marshallable.readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException{
//        Factory factory = Factory.getSingleton();
        try {
            for (Field field : this.getClass().getFields()) { // same order assumed across nodes
                if (! Modifier.isTransient(field.getModifiers()) &&
                        ! Modifier.isStatic(field.getModifiers())) {
                    Object value = objectInput.readObject();
                    field.set(this, value);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    declare parents: @javax.persistence.Entity * implements Marshallable;

}