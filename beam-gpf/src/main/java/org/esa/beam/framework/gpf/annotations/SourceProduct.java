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
     * @return {@code true} if the source product is optional.
     *         In this case the field value thus may be {@code null}.
     *         Defaults to {@code false}.
     */
    boolean optional() default false;

    /**
     * @return The product type or a regular expression identifying the allowed product types.
     *         Defaults to the empty string (= not set).
     * @see java.util.regex.Pattern
     */
    String type() default "";

    /**
     * @return The names of the bands which need to be present in the source product.
     *         Defaults to an empty array (= not set).
     */
    String[] bands() default {};

    /**
     * @return The alias identifier.
     *         Defaults to the empty string (= not set).
     */
    String alias() default "";

    /**
     * @return A brief description of the source product.
     *         Defaults to the empty string (= not set).
     */
    String description() default "";


    String label() default "";
}
