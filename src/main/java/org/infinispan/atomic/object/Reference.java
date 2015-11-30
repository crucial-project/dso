package org.infinispan.atomic.object;

import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.commons.api.BasicCache;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Pierre Sutra
 */
public class Reference<T> implements Externalizable {

   private Class<T> clazz;
   private Object key;

   public static Object unreference(Reference reference, AtomicObjectFactory factory) {
      return factory.getInstanceOf(reference);
   }

   public static Object unreference(Object arg, BasicCache cache) {
      return unreference(Collections.singleton(arg).toArray(),cache)[0];
   }

   public static Object[] unreference(Object[] args, BasicCache cache) {
      List<Object> ret = new ArrayList<>(args.length);
      for(Object arg : args) {
         if (arg instanceof Reference) {
            ret.add(unreference((Reference)arg, AtomicObjectFactory.forCache(cache)));
         } else {
            if (arg instanceof List) {
               List list = new ArrayList(((List) arg).size());
               for(int i = 0; i<((List) arg).size(); i++) {
                  Object item = ((List)arg).get(i);
                  list.add(unreference(item, cache));
               }
               ret.add(list);
            } else {
               ret.add(arg);
            }
         }
      }
      return ret.toArray();
   }

   // Object fields

   public Reference(){}
   
   public Reference(Class<T> c, Object key){
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

   // Care about Class.hashCode() not being portable ...
   @Override
   public int hashCode() {
      int result = (key != null ? key.hashCode() : 0);
      return result;
   }

   @Override
   public String toString(){
      return getClazz().getName().toString()+"#"+getKey().toString();
   }

   public Object getKey() {
      return key;
   }
   
   public Class getClazz(){ return clazz;}

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
