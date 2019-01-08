package org.infinispan.creson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author Pierre Sutra
 */

@Target(ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ReadOnly {
}
