package org.infinispan.atomic;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author Pierre Sutra
*/
@Aspect
public class Distribution {

   @Pointcut("call((@DistClass *).new(..)) " +
         "&& ! within(org.infinispan.atomic.container.BaseContainer) " +
         "&& ! within(org.infinispan.atomic.filter.ObjectFilterConverter)")
   public static void initDistributedClass(ProceedingJoinPoint pjp) {
   }

   @Pointcut("set(@DistField * *) ")
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

//   @Around("setDistributedField(pjp)"+
//         "&& ! within(org.infinispan.atomic.container.BaseContainer) " +
//         "&& ! within(org.infinispan.atomic.filter.ObjectFilterConverter)" +
//         "&& ! within(org.infinispan.atomic.Distribution)")
//   public void distributionAdviceField(ProceedingJoinPoint pjp) throws Throwable{
//      AtomicObjectFactory factory = AtomicObjectFactory.getSingleton();
//      pjp.proceed(pjp.getArgs());
//      Reference reference = referenceFor(pjp.getArgs()[0]);
//      for(Field field : pjp.getTarget().getClass().getDeclaredFields()){
//         if (Modifier.isPublic(field.getModifiers()) && field.get(pjp.getTarget()) == pjp.getArgs()[0]) {
//            field.set(pjp.getTarget(), factory.getInstanceOf(reference,true,null,false));
//            return;
//         }
//      }
//      throw new IllegalStateException("Field for "+pjp.getArgs()[0].getClass()+"="+pjp.getArgs()[0]+" is not public.");
//   }

}
