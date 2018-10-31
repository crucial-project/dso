package org.infinispan.crucial;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.infinispan.crucial.object.Reference;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.infinispan.crucial.Shared.SEPARATOR;

/**
 * @author Pierre Sutra
 */
@Aspect
public class Distribution{

    @Pointcut("call((@javax.persistence.Entity *).new(..)) " +
            "&& ! within(org.infinispan.crucial.container.BaseContainer)")
    public static void distributeEntity(ProceedingJoinPoint pjp){
    }

    @Pointcut("set(@org.infinispan.crucial.Shared * *)")
    public static void distributeField(ProceedingJoinPoint pjp){
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
        Class fieldClass = pjp.getArgs()[0].getClass();

        // parent class name or key if referencable?
        String parentClassOrReference =
                Reference.isReferencable(pjp.getThis().getClass()) ?
                        Reference.of(pjp.getThis()).toString() : pjp.getThis().getClass().getCanonicalName();

        Field field = pjp.getStaticPart().getSignature().getDeclaringType().getDeclaredField(fieldName);
        if (! Modifier.isStatic(field.getModifiers())) {
            String key = (! field.getAnnotation(Shared.class).key().equals(Shared.DEFAULT_KEY)) ?
                    field.getAnnotation(Shared.class).key()
                    : fieldName + SEPARATOR + parentClassOrReference;
            field.setAccessible(true);
            field.set(pjp.getTarget(), factory.getInstanceOf(
                    fieldClass,
                    key,
                    field.getAnnotation(Shared.class).readOptimization(),
                    field.getAnnotation(Shared.class).forceNew()));
            return;
        }
        throw new IllegalStateException("Field " + fieldName + " should not be static.");
    }

}
