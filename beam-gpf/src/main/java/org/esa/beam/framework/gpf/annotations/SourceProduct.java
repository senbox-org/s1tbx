package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SourceProduct {

    boolean optional() default false;  // todo - process

    // todo - String[] types() 

    String type() default "";  // todo - process

    String[] bands() default {};  // todo - process

    String alias() default "";  // todo - process
}
