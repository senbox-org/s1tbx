package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     *         Defaults to the empty string (= not set).
     */
    String version() default "";

    /**
     * @return The author(s) of the operator.
     *         Defaults to the empty string (= not set).
     */
    String authors() default "";

    /**
     * @return The copyright notice for the operator code.
     *         Defaults to the empty string (= not set).
     */
    String copyright() default "";

    /**
     * @return A brief description of the operator's purpose.
     *         Defaults to the empty string (= not set).
     */
    String description() default "";


    /**
     * @return If {@code true}, this operator is considered for internal use only and thus
     *         may not be exposed in user interfaces.
     */
    boolean internal() default false;
}
