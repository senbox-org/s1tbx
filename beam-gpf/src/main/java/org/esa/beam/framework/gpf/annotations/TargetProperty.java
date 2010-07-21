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

package org.esa.beam.framework.gpf.annotations;

import java.lang.annotation.*;

/**
 * Marks a target property field of an {@link org.esa.beam.framework.gpf.Operator Operator}.
 * The field must be computed during initialization of the operator.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TargetProperty {
    /**
     * @return A brief description of the target property.
     *         Defaults to the empty string (= not set).
     */
    String description() default "";

    /**
     * @return An alias name for the property.
     *         Defaults to the empty string (= not set).
     */
    String alias() default "";
}
