package org.infinispan.atomic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author Pierre Sutra
 */

@Target(ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Distributed {
   public boolean cacheable() default false;
}
