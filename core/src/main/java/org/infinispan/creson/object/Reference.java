package org.infinispan.creson.object;

import org.infinispan.creson.Shared;
import org.infinispan.creson.utils.Reflection;

import javax.persistence.Id;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;

/**
 * @author Pierre Sutra
 */
public class Reference<T> implements Externalizable {

    // Class methods

    @Deprecated
    public static Field getIDField(Class<?> clazz) {
        Field field = null;

        for (java.lang.reflect.Field f : Reflection.getAllFields(clazz)) {
            f.setAccessible(true);
            if (f.getAnnotation(Id.class) != null) {
                field = f;
                break;
            }
        }

        return field;
    }


    public static boolean isReferencable(Class<?> clazz) {
        return getIDField(clazz) != null;
    }

    public static <T> Reference<T> of(T object) throws IllegalAccessException {
        Class<T> clazz = (Class<T>) object.getClass();
        Field field = getIDField(clazz);
        if (field == null)
            throw new ClassFormatError("Missing key in "+clazz+" (fields= "
                    + Reflection.getAllFields(clazz)+")");
        field.setAccessible(true);
        return new Reference<>(clazz,field.get(object));

    }

    // Object fields & methods

    private Class<T> clazz;
    private Object key;

    @Deprecated
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
        return  getKey().toString() + Shared.SEPARATOR + getClazz().getCanonicalName();
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
