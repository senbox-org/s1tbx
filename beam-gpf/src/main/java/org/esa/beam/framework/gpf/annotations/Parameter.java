package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Validator;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Parameter {
    String interval() default ""; // e.g. "[10,20)"      // todo - process

    String condition() default ""; // e.g. "value > 2.5"      // todo - process

    String pattern() default ""; // e.g. "a*"                  // todo - process

    String format() default ""; // e.g. "yyyy-MM-dd HH:mm:ss.Z"       // todo - process

    String alias() default "";   // todo - process

    String defaultValue() default "";   // todo - process

    boolean notNull() default false;   // todo - process

    boolean notEmpty() default false;   // todo - process

    String label() default "";   // todo - process

    String unit() default "";   // todo - process

    String description() default "";   // todo - process

    String[] valueSet() default {};   // todo - process

    Class<? extends Validator> validator() default Validator.class;

    Class<? extends Converter> converter() default Converter.class;
}
