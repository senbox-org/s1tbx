package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

/**
 * Marks a target property field of an {@link org.esa.beam.framework.gpf.Operator Operator}.
 * The field must be computed during initialization of the operator.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TargetProperty {
    /**
     * @return A brief description of the target property.
     *         Defaults to the empty string (= not set).
     */
    String description() default "";

    /**
     * @return An alias name for the property.
     *         Defaults to the empty string (= not set).
     */
    String alias() default "";
}
