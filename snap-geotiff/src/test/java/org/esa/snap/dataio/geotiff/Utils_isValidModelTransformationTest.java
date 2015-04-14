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

package org.esa.snap.dataio.geotiff;

import org.junit.Test;

import static org.junit.Assert.*;

public class Utils_isValidModelTransformationTest {

    @Test
    public void testNull() {
        assertEquals(false, Utils.isValidModelTransformation(null));
    }

    @Test
    public void testBadArraySize() {
        assertEquals(false, Utils.isValidModelTransformation(new double[]{3, 4}));
    }

    @Test
    public void testAllValuesAreZero() {
        assertEquals(false, Utils.isValidModelTransformation(new double[]{
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0
        }));
    }

    @Test
    public void testValidTransformationValues() {
        assertEquals(true, Utils.isValidModelTransformation(new double[]{
                1, 2, 0, 3,
                4, 5, 0, 6,
                7, 8, 0, 9,
                0, 0, 0, 0
        }));
    }
}
