package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

/**
 * Marks a source product field of an {@link org.esa.beam.framework.gpf.Operator Operator}.
 * The field must be of type {@link org.esa.beam.framework.datamodel.Product Product}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SourceProduct {

    /**
     * @return {@code true} if the source product is optional (and the field value thus may be {@code null}).
     */
    boolean optional() default false;

    /**
     * @return The allowed product types.
     */
    String[] types() default {};

    /**
     * @return The bands which need to be present.
     */
    String[] bands() default {};

    /**
     * @return The alias identifier.
     */
    String alias() default "";
}
