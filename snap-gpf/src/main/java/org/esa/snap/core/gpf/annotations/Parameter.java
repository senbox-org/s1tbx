/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf.annotations;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.dom.DomConverter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a processing parameter field of an {@link Operator Operator}.
 * The field can be of any type.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Parameter {

    /**
     * @return An alias name for the parameter.
     *         Defaults to the empty string (= not set).
     */
    String alias() default "";

    /**
     * @return An alias name for the elements of a parameter array.
     *         Forces element-wise array conversion from and to DOM representation.
     *         Defaults to the empty string (= not set).
     */
    String itemAlias() default "";

    /**
     * Gets the parameter's default value.
     * The default value set is given as a textual representations of the actual value.
     * The framework creates the actual value set by converting the text value to
     * an object using the associated {@link Converter}.
     *
     * @return The default value.
     *         Defaults to the empty string (= not set).
     * @see #converter()
     */
    String defaultValue() default "";

    /**
     * @return A human-readable version of the name to be used in user interfaces.
     *         Defaults to the empty string (= not set).
     */
    String label() default "";

    /**
     * @return The parameter physical unit.
     *         Defaults to the empty string (= not set).
     */
    String unit() default "";

    /**
     * @return The parameter description.
     *         Defaults to the empty string (= not set).
     */
    String description() default "";

    /**
     * Gets the set of values which can be assigned to a parameter field.
     * The value set is given as textual representations of the actual values.
     * The framework creates the actual value set by converting each text value to
     * an object value using the associated {@link Converter}.
     *
     * @return The value set.Defaults to empty array (= not set).
     * @see #converter()
     */
    String[] valueSet() default {};

    /**
     * Gets the valid interval for numeric parameters, e.g. {@code "[10,20)"}: in the range 10 (inclusive) to 20 (exclusive).
     *
     * @return The valid interval. Defaults to empty string (= not set).
     */
    String interval() default "";

    /**
     * Gets a conditional expression which must return {@code true} in order to indicate
     * that the parameter value is valid, e.g. {@code "value > 2.5"}.
     *
     * @return A conditional expression. Defaults to empty string (= not set).
     */
    String condition() default "";

    /**
     * Gets a regular expression pattern to which a textual parameter value must match in order to indicate
     * a valid value, e.g. {@code "a*"}.
     *
     * @return A regular expression pattern. Defaults to empty string (= not set).
     * @see java.util.regex.Pattern
     */
    String pattern() default "";

    /**
     * Gets a format string to which a textual parameter value must match in order to indicate
     * a valid value, e.g. {@code "yyyy-MM-dd HH:mm:ss.Z"}.
     *
     * @return A format string. Defaults to empty string (= not set).
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
     * @return The validator class.
     */
    Class<? extends Validator> validator() default Validator.class;

    /**
     * A converter to be used to convert a text to the parameter value and vice versa.
     *
     * @return The converter class.
     */
    Class<? extends Converter> converter() default Converter.class;

    /**
     * A converter to be used to convert an (XML) DOM to the parameter value and vice versa.
     *
     * @return The DOM converter class.
     */
    Class<? extends DomConverter> domConverter() default DomConverter.class;

     /**
     * Specifies which {@code RasterDataNode} subclass of the source products is used 
     * to fill the {@link #valueSet()} for this parameter.
     * 
     * @return The raster data node type.
     */
    Class<? extends RasterDataNode> rasterDataNodeType() default RasterDataNode.class;


    /**
     * @return An arbitrary Boolean value which will be ignored.
     * @see #itemAlias()
     * @deprecated Since BEAM 5. Not used anymore. No replacement.
     */
    @Deprecated
    boolean itemsInlined() default false;
}
