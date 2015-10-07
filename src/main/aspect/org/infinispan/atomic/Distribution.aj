package org.infinispan.atomic;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

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
      for (Field field : object.getClass().getDeclaredFields()) {
         if (field.isAnnotationPresent(Key.class)) {
            AtomicObjectFactory factory = AtomicObjectFactory.forCache("");
            return factory.getInstanceOf(object.getClass(), field.get(object), true, null, false, pjp.getArgs());
         }
      }
      throw new IllegalStateException("Key field is missing.");
   }

}
