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
 * The class {@code FracIndex} is a simple representation of
 * an index with an integral and a fractional component.
 */
public final class FracIndex {

    /**
     * The integral component.
     */
    public int i;
    /**
     * The fractional component.
     */
    public double f;

    /**
     * Creates an array of type {@code FracIndex[]}.
     *
     * @param length the length of the array being created.
     * @return the created array.
     */
    public static FracIndex[] createArray(int length) {
        final FracIndex[] fracIndexes = new FracIndex[length];

        for (int i = 0; i < length; i++) {
            fracIndexes[i] = new FracIndex();
        }

        return fracIndexes;
    }

    /**
     * Sets the fractional component to 0.0 if it is less than
     * zero, and to 1.0 if it is greater than unity.
     */
    public final void truncate() {
        if (f < 0.0) {
            f = 0.0;
        } else if (f > 1.0) {
            f = 1.0;
        }
    }
}
