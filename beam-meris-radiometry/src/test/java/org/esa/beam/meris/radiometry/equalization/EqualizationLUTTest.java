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

package org.esa.beam.meris.radiometry.equalization;

import org.esa.beam.preprocessor.equalization.EqualizationLUT;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static junit.framework.Assert.*;

public class EqualizationLUTTest {

    @Test
    public void testGetCoefficients() throws IOException {
        Reader[] readers = new Reader[2];
        readers[0] = new StringReader("1.0 2.0 3.0\n4.0 5.0 6.0");
        readers[1] = new StringReader("0.1 0.2 0.3\n0.4 0.5 0.6");
        final EqualizationLUT lut = new EqualizationLUT(readers);

        double[] expected = {1.0, 2.0, 3.0};
        compare(expected, lut.getCoefficients(0, 0));

        expected = new double[]{4.0, 5.0, 6.0};
        compare(expected, lut.getCoefficients(0, 1));

        expected = new double[]{0.1, 0.2, 0.3};
        compare(expected, lut.getCoefficients(1, 0));

        expected = new double[]{0.4, 0.5, 0.6};
        compare(expected, lut.getCoefficients(1, 1));
    }


    private void compare(double[] expected, double[] actualCoefficients) {
        assertEquals(expected[0], actualCoefficients[0]);
        assertEquals(expected[1], actualCoefficients[1]);
        assertEquals(expected[2], actualCoefficients[2]);
    }

}
