package org.infinispan.atomic;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.infinispan.atomic.object.Reference;

import java.lang.reflect.Field;

/**
 * @author Pierre Sutra
*/
@Aspect
public class Distribution {

   @Pointcut("call((@Distributed *).new(..)) " +
         "&& ! within(org.infinispan.atomic.container.BaseContainer) " +
         "&& ! within(org.infinispan.atomic.filter.ObjectFilterConverter)")
   public static void initDistributedObject(ProceedingJoinPoint pjp) {
   }

   @Around("initDistributedObject(pjp)")
   public Object distributionAdvice(ProceedingJoinPoint pjp) throws Throwable{
      Object object = pjp.proceed(pjp.getArgs());
      Reference reference = referenceFor(object);
      AtomicObjectFactory factory = AtomicObjectFactory.getSingleton();
      return factory.getInstanceOf(
            reference,
            true,
            null,
            false,
            pjp.getArgs());

   }

   public static Reference referenceFor(Object object) throws IllegalAccessException {
      for (Field field : object.getClass().getDeclaredFields()) {
         if (field.isAnnotationPresent(Key.class)) {
            return new Reference(
                  object.getClass(),
                  field.get(object));
         }
      }
      throw new IllegalStateException("Key field is missing.");
   }


   public static boolean isDistributed(Object object) {
      for (Field field : object.getClass().getDeclaredFields()) {
         if (field.isAnnotationPresent(Key.class)) {
            return true;
         }
      }
      return false;
   }

}
