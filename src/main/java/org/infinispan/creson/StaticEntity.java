package org.infinispan.creson;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StaticEntity {
    String name() default "";
}
