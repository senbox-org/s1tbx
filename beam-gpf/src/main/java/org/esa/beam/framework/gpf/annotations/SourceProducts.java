package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

/**
 * Marks a source product array field of an {@link org.esa.beam.framework.gpf.Operator Operator}.
 * The field must be of type {@link org.esa.beam.framework.datamodel.Product Product}{@code []}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SourceProducts {

    /**
     * @return the number of source products expected.
     *         The value {@code -1} means any number but at least one source product.
     *         Defaults to {@code 0} (= not set).
     */
    int count() default 0;

    /**
     * @return The regular expression identifying the allowed product types.
     *         Defaults to the empty string (= not set).
     */
    String type() default "";

    /**
     * @return The names of the bands which need to be present in the source product.
     *         Defaults to an empty array (= not set).
     */
    String[] bands() default {};

    /**
     * @return A brief description of the source products array.
     *         Defaults to the empty string (= not set).
     */
    String description() default "";
}
