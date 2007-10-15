package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

/**
 * Provides metadata for an operator. This annotation is valid for
 * extensions of the {@link org.esa.beam.framework.gpf.Operator Operator} class.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OperatorMetadata {
    /**
     * @return An alias name for the operator.
     */
    String alias();

    /**
     * @return The version of the operator.
     */
    String version() default "1.0";

    /**
     * @return The author(s) of the operator.
     */
    String authors() default "";

    /**
     * @return The copyright notice for the operator code.
     */
    String copyright() default "";

    /**
     * @return A brief description of the operator's purpose.
     */
    String description() default "";
}
