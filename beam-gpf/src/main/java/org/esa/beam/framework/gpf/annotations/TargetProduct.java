package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

/**
 * Marks the target product field of an {@link org.esa.beam.framework.gpf.Operator Operator}.
 * The field must be of type {@link org.esa.beam.framework.datamodel.Product Product}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TargetProduct {
    /**
     * @return A brief description of the target product.
     *         Defaults to the empty string (= not set).
     */
    String description() default "";
}
