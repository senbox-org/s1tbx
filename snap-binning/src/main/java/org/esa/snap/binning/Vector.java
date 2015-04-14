/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning;

/**
 * A vector of {@code float} elements.
 *
 * @author Norman Fomferra
 */
public interface Vector {
    /**
     * @return The size of the vector (number of elements).
     */
    int size();

    /**
     * Gets the {@code float} element at the given index.
     *
     * @param index The element index.
     * @return The {@code float} element at the given index.
     */
    float get(int index);
}
