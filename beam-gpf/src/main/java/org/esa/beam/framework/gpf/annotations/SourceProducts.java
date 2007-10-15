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
     * @return the number of source products expected. A return value of {@code -1} signals an unlimited number of sources.
     */
    int count() default -1;

    /**
     * @return The allowed product types.
     */
    String[] types() default {};

    /**
     * @return The bands which need to be present.
     */
    String[] bands() default {};
}
