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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides metadata for an operator. This annotation is valid for
 * extensions of the {@link Operator Operator} class.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OperatorMetadata {

    /**
     * @return A human-readable version of the name to be used in user interfaces.
     * Defaults to the empty string (= not set).
     */
    String label() default "";

    /**
     * @return An alias name for the operator.
     */
    String alias();

    /**
     * @return The version of the operator.
     * Defaults to the empty string (= not set).
     */
    String version() default "";

    /**
     * @return The author(s) of the operator.
     * Defaults to the empty string (= not set).
     */
    String authors() default "";

    /**
     * @return The copyright notice for the operator code.
     * Defaults to the empty string (= not set).
     */
    String copyright() default "";

    /**
     * @return A brief description of the operator's purpose.
     * Defaults to the empty string (= not set).
     */
    String description() default "";

    /**
     * @return The category/context where the operator is used.
     * Defaults to the empty string (= not set).
     */
    String category() default "";

    /**
     * The GPF framework usually writes the target product of either single operators or processing graphs to the file
     * system when executed from the GPT command-line interface or the operator GUI.
     * <p>
     * If the {@code autoWriteDisabled} property is set, this default behaviour is switched off and hence,
     * the operator or graph is responsible for outputting any computed results.
     * <p>
     * Setting this property on an operator will only be useful, if it either does not generate a new target
     * {@link Product Product} and/or if it
     * does its own outputting of non-{@code Product} targets to external files.
     *
     * @return If {@code true}, the framework will prevent automatic writing of the target product to the file system.
     * @since BEAM 5.0
     */
    boolean autoWriteDisabled() default false;

    /**
     * @return If {@code true}, this operator is considered for internal use only and thus
     * may not be exposed in user interfaces.
     */
    boolean internal() default false;

}
