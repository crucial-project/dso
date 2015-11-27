package org.infinispan.atomic.object;

import org.infinispan.atomic.DistClass;
import org.infinispan.atomic.ReadOnly;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.marshall.core.JBossMarshaller;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Pierre Sutra
 */
public class Utils {

   private static Log log = LogFactory.getLog(Utils.class);

   private static Map<Class,Set<Method>> unsupportedMethods = new HashMap<>();
   static {
      try {
         unsupportedMethods.put(AbstractCollection.class, new HashSet<Method>());
         unsupportedMethods.get(AbstractCollection.class).add(Collection.class.getDeclaredMethod("iterator"));
      } catch (NoSuchMethodException e) {
         e.printStackTrace();
      }
   }

   public static Object getMethod(Object obj, String method, Object[] args)
         throws IllegalAccessException {
      for (Method m : obj.getClass().getMethods()) { // only public methods (inherited and not)
         if (method.equals(m.getName())) {
            if (m.getGenericParameterTypes().length == args.length) {
               if (isCompatible(m,args)){
                  return m;
               }
            }
         }
      }
      throw new IllegalStateException("Method " + method + " not found.");
   }

   public static boolean hasReadOnlyMethods(Class clazz){
      for (Method m : clazz.getMethods()) { // only public methods (inherited and not)
         if (m.isAnnotationPresent(ReadOnly.class))
            return true;
      }
      return false;
   }

   public static boolean isMethodSupported(Class clazz, Method method) {
      if (clazz.equals(Object.class))
         return true;
      if (unsupportedMethods.containsKey(clazz))
         if (unsupportedMethods.get(clazz).contains(method))
            return false;
      return isMethodSupported(clazz.getSuperclass(), method);
   }


   public static boolean hasDefaultConstructor(Class clazz){
      for (Constructor constructor : clazz.getConstructors()) {
         if (constructor.getParameterTypes().length==0)
            return true;
      }
      return false;
   }

   public static boolean isDistributed(Class clazz){
      return clazz.isAnnotationPresent(DistClass.class);
   }
   
   public static Object callObject(Object obj, String method, Object[] args)
         throws InvocationTargetException, IllegalAccessException {
      for (Method m : obj .getClass().getMethods()) { // only public methods (inherited and not)
         if (method.equals(m.getName())) {
            if (m.getParameterTypes().length == args.length) {
               if (isCompatible(m, args))
                  return m.invoke(obj, args);
            }
         }
      }
      throw new IllegalStateException("Method "+method+" not found.");
   }

   public static Object initObject(Class clazz, Object... initArgs)
         throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
      Constructor[] allConstructors = clazz.getDeclaredConstructors();
      for (Constructor ctor : allConstructors) {
         if (ctor.getParameterTypes().length == initArgs.length) {
            if (isCompatible(ctor, initArgs)) {
               if (log.isTraceEnabled()) log.trace("new " + clazz.toString() + "(" + Arrays.toString(initArgs) + ")");
               return ctor.newInstance(initArgs);
            }
         }
      }
      throw new IllegalArgumentException("Unable to find constructor for "+clazz.toString()+" with "+Arrays.toString(initArgs));
   }

   /**
    * @author Sean Patrick Floyd
    * @param method
    * @param params
    * @return
    * @throws IllegalAccessException
    */
   public static boolean isCompatible(final Method method, final Object[] params)
         throws IllegalAccessException {
      final Class<?>[] parameterTypes = method.getParameterTypes();
      if(params.length != parameterTypes.length){
         return false;
      }
      for(int i = 0; i < params.length; i++){
         final Object object = params[i];
         final Class<?> paramType = parameterTypes[i];
         if(!isCompatible(object, paramType)){
            return false;
         }
      }
      return true;
   }

   /**
    * @author Sean Patrick Floyd
    * @param constructor
    * @param params
    * @return
    * @throws IllegalAccessException
    */
   public static boolean isCompatible(final Constructor constructor, final Object[] params)
         throws IllegalAccessException {
      final Class<?>[] parameterTypes = constructor.getParameterTypes();
      if(params.length != parameterTypes.length){
         return false;
      }
      for(int i = 0; i < params.length; i++){
         final Object object = params[i];
         final Class<?> paramType = parameterTypes[i];
         if(!isCompatible(object, paramType)){
            return false;
         }
      }
      return true;
   }

   /**
    * @author Sean Patrick Floyd
    * @param object
    * @param paramType
    * @return
    * @throws IllegalAccessException
    */
   private static boolean isCompatible(final Object object, final Class<?> paramType)
         throws IllegalAccessException {
      if(object == null){
         // primitive parameters are the only parameters
         // that can't handle a null object
         return !paramType.isPrimitive();
      }
      // handles same type, super types and implemented interfaces
      if(paramType.isInstance(object)){
         return true;
      }
      // special case: the arg may be the Object wrapper for the
      // primitive parameter type
      if(paramType.isPrimitive()){
         return isWrapperTypeOf(object.getClass(), paramType);
      }
      return false;

   }

   /**
    * @author Sean Patrick Floyd
    * @param candidate
    * @param primitiveType
    * @return
    * @throws IllegalAccessException
    */
   private static boolean isWrapperTypeOf(final Class<?> candidate, final Class<?> primitiveType)
         throws IllegalAccessException {
      try{
         return !candidate.isPrimitive()
               && candidate
               .getDeclaredField("TYPE")
               .get(null)
               .equals(primitiveType);
      } catch(final NoSuchFieldException e){
         return false;
      }
   }

   private static boolean DEFAULT_BOOLEAN;
   private static byte DEFAULT_BYTE;
   private static short DEFAULT_SHORT;
   private static int DEFAULT_INT;
   private static long DEFAULT_LONG;
   private static float DEFAULT_FLOAT;
   private static double DEFAULT_DOUBLE;

   public static Object getDefaultValue(Class clazz) {
      if (clazz.equals(boolean.class)) {
         return DEFAULT_BOOLEAN;
      } else if (clazz.equals(byte.class)) {
         return DEFAULT_BYTE;
      } else if (clazz.equals(short.class)) {
         return DEFAULT_SHORT;
      } else if (clazz.equals(int.class)) {
         return DEFAULT_INT;
      } else if (clazz.equals(long.class)) {
         return DEFAULT_LONG;
      } else if (clazz.equals(float.class)) {
         return DEFAULT_FLOAT;
      } else if (clazz.equals(double.class)) {
         return DEFAULT_DOUBLE;
      } else {
         throw new IllegalArgumentException(
               "Class type " + clazz + " not supported");
      }
   }

   public static byte[] marshall(Object object) {
      Marshaller marshaller = new JBossMarshaller();
      try {
         if (object instanceof byte[])
            return (byte[]) object;
         return marshaller.objectToByteBuffer(object);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
   }

   public static Object unmarshall(Object object) {
      Marshaller marshaller = new JBossMarshaller();
      try {
         if (object instanceof byte[])
            return marshaller.objectFromByteBuffer((byte[]) object);
         return object;
      } catch (Exception e){
         e.printStackTrace();
      }
      return null;
   }

}
