package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SourceProducts {

    boolean optional() default false;  // todo - process
}
