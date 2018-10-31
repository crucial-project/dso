package org.infinispan.crucial;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Shared {
    String DEFAULT_KEY = "__none";
    String SEPARATOR = "#";
    boolean readOptimization() default true;
    boolean forceNew() default false;
    String key() default DEFAULT_KEY;
}
