package org.infinispan.creson;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author Pierre Sutra
*/
@Aspect
public class DistributionClass {

   @Pointcut("call((@javax.persistence.Entity *).new(..)) " +
         "&& ! within(org.infinispan.creson.container.BaseContainer)")
   public static void distributeEntity(ProceedingJoinPoint pjp) {
   }

   @Around("distributeEntity(pjp)")
   public Object distributionAdviceClass(ProceedingJoinPoint pjp) throws Throwable{
      Factory factory = Factory.getSingleton();
      return factory.getInstanceOf(
              pjp.getStaticPart().getSignature().getDeclaringType(),
              null,
              false,
              false,
              pjp.getArgs());
   }

}
