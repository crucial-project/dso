package org.infinispan.creson;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.infinispan.creson.utils.ContextManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.infinispan.creson.object.Reference.SEPARATOR;

/**
 * @author Pierre Sutra
*/
@Aspect
public class Distribution {

   @Pointcut("call((@javax.persistence.Entity *).new(..)) " +
         "&& ! within(org.infinispan.creson.container.BaseContainer)")
   public static void distributeEntity(ProceedingJoinPoint pjp) {
   }

   @Pointcut("set(@org.infinispan.creson.Shared * *)")
   public static void distributeField(ProceedingJoinPoint pjp) {
   }

   @Around("distributeEntity(pjp)")
   public Object distributionAdviceClass(ProceedingJoinPoint pjp) throws Throwable{
      Factory factory = Factory.getSingleton();
      return factory.getInstanceOf(
            pjp.getStaticPart().getSignature().getDeclaringType(),
            null,
            true,
            false,
            pjp.getArgs());
   }

   @Around("distributeField(pjp)")
   public void distributionAdviceField(ProceedingJoinPoint pjp) throws Throwable{
      Factory factory = Factory.getSingleton();
      String fieldName = pjp.getStaticPart().getSignature().getName();
      String parentReferenceName = (ContextManager.getContext()!=null) ?
              ContextManager.getContext().getReference().toString() :
              pjp.getThis().getClass().getCanonicalName();
      Field field = pjp.getStaticPart().getSignature().getDeclaringType().getDeclaredField(fieldName);
      if (!Modifier.isStatic(field.getModifiers())) {
         field.setAccessible(true);
         field.set(pjp.getTarget(), factory.getInstanceOf(pjp.getArgs()[0].getClass(), fieldName+ SEPARATOR + parentReferenceName, false, field.getAnnotation(Shared.class).forceNew()));
         return;
      }
      throw new IllegalStateException("Field "+fieldName+" should not be static.");
   }

}
