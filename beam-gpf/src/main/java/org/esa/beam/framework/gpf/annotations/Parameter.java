package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Validator;

import java.lang.annotation.*;

/**
 * Marks a processing parameter field of an {@link org.esa.beam.framework.gpf.Operator Operator}.
 * The field can be of any type.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Parameter {
    /**
     * Gets an alias name for the parameter.
     *
     * @return An alias name, defaults to the empty string (= not set).
     */
    String alias() default "";

    /**
     * Gets the parameter's default value.
     * The default value set is given as a textual representations of the actual value.
     * The framework creates the actual value set by converting the text value to
     * an object using the associated {@link Converter}.
     *
     * @return The default value, defaults to the empty string (= not set).
     * @see #converter()
     */
    String defaultValue() default "";

    /**
     * The label for the parameter.
     *
     * @return The label, defaults to the empty string (= not set).
     */
    String label() default "";

    /**
     * The physical unit for the parameter.
     *
     * @return The unit, defaults to the empty string (= not set).
     */
    String unit() default "";

    /**
     * The description for the parameter.
     *
     * @return The description, defaults to the empty string (= not set).
     */
    String description() default "";

    /**
     * Gets the set of values which can be assigned to a parameter field.
     * The value set is given as textual representations of the actual values.
     * The framework creates the actual value set by converting each text value to
     * an object value using the associated {@link Converter}.
     *
     * @return The value set, defaults to empty array (= not set).
     * @see #converter()
     */
    String[] valueSet() default {};

    /**
     * Gets the valid interval for numeric parameters, e.g. {@code "[10,20)"}: in the range 10 (inclusive) to 20 (exclusive).
     *
     * @return The valid interval, defaults to empty string (= not set).
     */
    String interval() default "";

    /**
     * Gets a conditional expression which must return {@code true} in order to indicate
     * that the parameter value is valid, e.g. {@code "value > 2.5"}.
     *
     * @return A conditional expression, defaults to empty string (= not set).
     */
    String condition() default "";

    /**
     * Gets a regular expression pattern to which a textual parameter value must match in order to indicate
     * a valid value, e.g. {@code "a*"}.
     *
     * @return A regular expression pattern, defaults to empty string (= not set).
     * @see java.util.regex.Pattern
     */
    String pattern() default "";

    /**
     * Gets a format string to which a textual parameter value must match in order to indicate
     * a valid value, e.g. {@code "yyyy-MM-dd HH:mm:ss.Z"}.
     *
     * @return A format string, defaults to empty string (= not set).
     * @see java.text.Format
     */
    String format() default "";


    /**
     * Parameter value must not be {@code null}?
     *
     * @return {@code true}, if so. Defaults to {@code false}.
     */
    boolean notNull() default false;

    /**
     * Parameter value must not be an empty string?
     *
     * @return {@code true}, if so. Defaults to {@code false}.
     */
    boolean notEmpty() default false;

    /**
     * A validator to be used to validate a parameter value.
     *
     * @return The validator, defaults to {@code Validator.class} (= not set).
     */
    Class<? extends Validator> validator() default Validator.class;

    /**
     * A validator to be used to convert a text to the parameter value and vice versa.
     *
     * @return The converter, defaults to {@code Converter.class} (= not set).
     */
    Class<? extends Converter> converter() default Converter.class;
}
