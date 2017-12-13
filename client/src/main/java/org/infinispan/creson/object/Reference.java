package org.infinispan.creson.object;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Pierre Sutra
 */
public class Reference<T> implements Externalizable {

    public static String SEPARATOR = "#";
    private Class<T> clazz;
    private Object key;

    // Object fields

    public Reference() {
    }

    public Reference(Class<T> c, Object key) {
        clazz = c;
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Reference<?> reference = (Reference<?>) o;

        if (clazz != null ? !clazz.equals(reference.clazz) : reference.clazz != null)
            return false;
        return !(key != null ? !key.equals(reference.key) : reference.key != null);

    }

    // FIXME Class.hashCode() not being portable ...
    @Override
    public int hashCode() {
        int result = (key != null ? key.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return  getKey().toString() + SEPARATOR + getClazz().getCanonicalName();
    }

    public Object getKey() {
        return key;
    }

    public Class getClazz() {
        return clazz;
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeObject(clazz);
        objectOutput.writeObject(key);
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        clazz = (Class) objectInput.readObject();
        key = objectInput.readObject();
    }

}
