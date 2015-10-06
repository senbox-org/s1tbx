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

package org.esa.snap.core.util.math;

/**
 * An interface used as parameter to several methods which perform some actions on data arrays.
 * It is used to decide whether or not an array value shall be taken into account for a particular
 * computation.
 */
public interface IndexValidator {

    /**
     * The validator whose {@link #validateIndex} method always returns true.
     */
    IndexValidator TRUE = new IndexValidator() {
                public final boolean validateIndex(int index) {
                    return true;
                }
            };

    /**
     * If the given <code>index</code> or the value at the given <code>index</code> is valid, this method should return
     * <code>true</code>, otherwise <code>false</code>.
     *
     * @param index the index to validate
     *
     * @return <code>true</code>, if the index or the data behind the index is valid.
     */
    boolean validateIndex(int index);
}
