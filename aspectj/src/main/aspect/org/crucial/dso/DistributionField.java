package org.crucial.dso;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.crucial.dso.object.Reference;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Aspect
public class DistributionField {

    // FIXME constructor?
    @Pointcut("set(@org.crucial.dso.Shared * *)")
    public static void distributeField(ProceedingJoinPoint pjp) {
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
        if (!Modifier.isStatic(field.getModifiers())) {
            String key = (!field.getAnnotation(Shared.class).key().equals(Shared.DEFAULT_KEY)) ?
                    field.getAnnotation(Shared.class).key()
                    : fieldName + Shared.SEPARATOR + parentClassOrReference;
            field.setAccessible(true);
            field.set(pjp.getTarget(), factory.getInstanceOf(
                    fieldClass,
                    key,
                    field.getAnnotation(Shared.class).readOptimization(),
                    field.getAnnotation(Shared.class).isIdempotent(),
                    field.getAnnotation(Shared.class).forceNew()));
            return;
        }
        throw new IllegalStateException("Field "+fieldName+" should not be static.");
    }


}
