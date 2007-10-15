package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OperatorMetadata {
    String alias();

    String version() default "1.0";

    String author() default "";

    String copyright() default "";

    String description() default "";
}
