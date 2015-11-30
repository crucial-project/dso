package org.infinispan.atomic;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Pierre Sutra
*/
@Aspect
public class Distribution {

   @Pointcut("call((@Distributed *).new(..)) " +
         "&& ! within(org.infinispan.atomic.container.BaseContainer)" +
         "&& ! within(org.infinispan.atomic.filter.ObjectFilterConverter)")
   public static void initDistributedClass(ProceedingJoinPoint pjp) {
   }

   @Pointcut("set(@Distribute * *) ")
   public static void setDistributedField(ProceedingJoinPoint pjp) {
   }

   @Around("initDistributedClass(pjp)")
   public Object distributionAdviceClass(ProceedingJoinPoint pjp) throws Throwable{
      AtomicObjectFactory factory = AtomicObjectFactory.getSingleton();
      return factory.getInstanceOf(
            pjp.getStaticPart().getSignature().getDeclaringType(),
            null,
            true,
            false,
            pjp.getArgs());
   }

   @Around("setDistributedField(pjp)")
   public void distributionAdviceField(ProceedingJoinPoint pjp) throws Throwable{
      AtomicObjectFactory factory = AtomicObjectFactory.getSingleton();
      String fieldName = pjp.getStaticPart().getSignature().getName();
      Field field = pjp.getStaticPart().getSignature().getDeclaringType().getField(fieldName);
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
         String key = field.getAnnotation(Distribute.class).key();
         field.set(pjp.getTarget(), factory.getInstanceOf(pjp.getArgs()[0].getClass(), key, true, false));
         return;
      }
      throw new IllegalStateException("Distributed fields for "+pjp.getTarget().getClass()+" must be both public and static.");
   }

}
