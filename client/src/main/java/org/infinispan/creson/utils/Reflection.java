package org.infinispan.creson.utils;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.creson.ReadOnly;
import org.infinispan.creson.object.Reference;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Pierre Sutra
 */
public class Reflection {

    private static Log log = LogFactory.getLog(Reflection.class);

    private static Map<Class, Set<Method>> unsupportedMethods = new HashMap<>();

    static {
        try {
            unsupportedMethods.put(AbstractCollection.class, new HashSet<>());
            unsupportedMethods.get(AbstractCollection.class).add(Collection.class.getDeclaredMethod("iterator"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static java.lang.Object open(Reference reference, java.lang.Object[] initArgs, BasicCache cache)
            throws IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException, NoSuchFieldException {

        java.lang.Object ret = instantiate(
                reference.getClazz(),Reference.unreference(initArgs,cache));

        // force the key field to the value in the reference
        if (reference.getClazz().getAnnotation(Entity.class)!=null) {
            assert reference.getKey() != null;
            java.lang.reflect.Field field = null;
            for (java.lang.reflect.Field f : reference.getClazz().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getAnnotation(Id.class) != null) {
                    field = f;
                    break;
                }
            }
            assert field != null : reference;
            field.set(ret, reference.getKey());
        }

        return ret;

    }

    public static java.lang.Object instantiate(Class clazz, java.lang.Object... initArgs)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (log.isTraceEnabled())
            log.trace("new " + clazz.toString() + "(" + Arrays.toString(initArgs) + ")");
        return constructor(clazz, initArgs).newInstance(initArgs);
    }

    public static Constructor constructor(Class clazz, java.lang.Object... initArgs)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Constructor[] allConstructors = clazz.getDeclaredConstructors();
        for (Constructor ctor : allConstructors) {
            ctor.setAccessible(true);
            if (ctor.getParameterTypes().length == initArgs.length) {
                if (isCompatible(ctor, initArgs)) {
                    return ctor;
                }
            }
        }
        throw new IllegalArgumentException("Unable to find constructor for " + clazz.toString() + " with " + Arrays.toString(initArgs));
    }

    // methods

    public static java.lang.Object callObject(java.lang.Object obj, String method, java.lang.Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        for (Method m : obj.getClass().getDeclaredMethods()) {
            m.setAccessible(true);
            if (method.equals(m.getName())) {
                if (m.getParameterTypes().length == args.length) {
                    if (isCompatible(m, args))
                        return m.invoke(obj, args);
                }
            }
        }
        throw new IllegalStateException("Method " + method + " not found.");
    }

    public static boolean hasReadOnlyMethods(Class clazz) {
        for (Method m : clazz.getMethods()) { // only public methods (inherited and not)
            if (m.isAnnotationPresent(ReadOnly.class))
                return true;
        }
        return false;
    }

    public static boolean isMethodSupported(Class clazz, Method method) {
        if (clazz.equals(java.lang.Object.class))
            return true;
        if (unsupportedMethods.containsKey(clazz))
            if (unsupportedMethods.get(clazz).contains(method))
                return false;
        return isMethodSupported(clazz.getSuperclass(), method);
    }

    /**
     * @param method
     * @param params
     * @return
     * @throws IllegalAccessException
     * @author Sean Patrick Floyd
     */
    public static boolean isCompatible(final Method method, final java.lang.Object[] params)
            throws IllegalAccessException {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (params.length != parameterTypes.length) {
            return false;
        }
        for (int i = 0; i < params.length; i++) {
            final java.lang.Object object = params[i];
            final Class<?> paramType = parameterTypes[i];
            if (!isCompatible(object, paramType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param constructor
     * @param params
     * @return
     * @throws IllegalAccessException
     * @author Sean Patrick Floyd
     */
    public static boolean isCompatible(final Constructor constructor, final java.lang.Object[] params)
            throws IllegalAccessException {
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        if (params.length != parameterTypes.length) {
            return false;
        }
        for (int i = 0; i < params.length; i++) {
            final java.lang.Object object = params[i];
            final Class<?> paramType = parameterTypes[i];
            if (!isCompatible(object, paramType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param object
     * @param paramType
     * @return
     * @throws IllegalAccessException
     * @author Sean Patrick Floyd
     */
    public static boolean isCompatible(final java.lang.Object object, final Class<?> paramType)
            throws IllegalAccessException {
        if (object == null) {
            // primitive parameters are the only parameters
            // that can't handle a null object
            return !paramType.isPrimitive();
        }
        // handles same type, super types and implemented interfaces
        if (paramType.isAssignableFrom(object.getClass())) {
            return true;
        }
        // special case: the arg may be the object wrapper for the
        // primitive parameter type
        if (paramType.isPrimitive()) {
            return isWrapperTypeOf(object.getClass(), paramType);
        }
        return false;

    }

    /**
     * @param candidate
     * @param primitiveType
     * @return
     * @throws IllegalAccessException
     * @author Sean Patrick Floyd
     */
    private static boolean isWrapperTypeOf(final Class<?> candidate, final Class<?> primitiveType)
            throws IllegalAccessException {
        try {
            return !candidate.isPrimitive()
                    && candidate
                    .getDeclaredField("TYPE")
                    .get(null)
                    .equals(primitiveType);
        } catch (final NoSuchFieldException e) {
            return false;
        }
    }

}
