package org.infinispan.creson.object;

import org.infinispan.creson.Factory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Pierre Sutra
 */
public class Reference<T> implements Externalizable {

    public static String SEPARATOR = "#";
    private Class<T> clazz;
    private Object key;

    public static Object unreference(Reference reference, Factory factory) {
        assert factory != null;
        return factory.getInstanceOf(reference);
    }

    public static Object unreference(Object arg, Factory factory) throws IllegalAccessException, InstantiationException {
        if (arg == null) return null;
        return unreference(Collections.singleton(arg).toArray(), factory)[0];
    }

    public static Object[] unreference(Object[] args, Factory factory) throws IllegalAccessException, InstantiationException {
        Object[] ret = new Object[args.length];
        for (int i=0; i<args.length; i++) {
            if (args[i] instanceof Reference) {
                ret[i] = unreference((Reference) args[i], factory);
            } else {
                Object object = args[i];
                if (object == null) {
                    ret[i] = null;
                } else if (object.getClass().isPrimitive()) {
                    ret[i] = args[i];
                } else if (object.getClass().isArray()) {
                    Object[] array = (Object[])object;
                    Object[] copy = new Object[array.length];
                    for (int j=0; j<copy.length; j++) {
                        copy[j] = unreference(array[j], factory);
                    }
                    ret[i] = copy;
                } else if (Collection.class.isAssignableFrom(object.getClass())){
                    Collection collection = (Collection) object.getClass().newInstance();
                    for(Object element: (Collection)object) {
                        collection.add(element);
                    }
                    ret[i] = collection;
                } else {
                    // FIXME
                    ret[i] = object;
                }
            }
        }
        return ret;
    }

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
