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
 * Marks a source product array field of an {@link Operator Operator}.
 * The field must be of type {@link Product Product}{@code []}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SourceProducts {
    /**
     * @return The alias identifier.
     *         Defaults to the empty string (= not set).
     */
    String alias() default "";

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

    /**
     * @return A human-readable version of the name to be used in user interfaces.
     *         Defaults to the empty string (= not set).
     */
    String label() default "";

    /**
     * @return If true, the operator supports multisize-products
     */
    boolean canBeMultisize() default true;
}
