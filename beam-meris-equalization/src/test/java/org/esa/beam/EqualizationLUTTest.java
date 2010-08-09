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

package org.esa.beam;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static junit.framework.Assert.*;

public class EqualizationLUTTest {

    @Test
    public void testGetCoefficientsReproTwo() throws IOException {
        final EqualizationLUT lut = new EqualizationLUT(2);

        double[] expected = {1.000340342521667E+00, 3.734220115347853E-07, -2.535939791492581E-10};
        assertTrue(Arrays.equals(expected, lut.getCoefficients(0, 0)));

        expected = new double[]{9.998928904533386E-01, -2.022538865276147E-07, 6.538485863849530E-10};
        assertTrue(Arrays.equals(expected, lut.getCoefficients(0, 563)));

        expected = new double[]{1.000470042228699E+00, 8.029708737922192E-07, -2.639678475802043E-10};
        assertTrue(Arrays.equals(expected, lut.getCoefficients(0, 923)));

        expected = new double[]{1.000318050384521E+00, -2.895004058700579E-07, -6.288190801395643E-11};
        assertTrue(Arrays.equals(expected, lut.getCoefficients(0, 111)));
    }

    @Test
    public void testGetCoefficientsReproThree() throws IOException {
        final EqualizationLUT lut = new EqualizationLUT(3);

        double[] expected = {1.001895189285278E+00, -2.161935981348506E-06, 7.555084335919560E-10};
        assertTrue(Arrays.equals(expected, lut.getCoefficients(14, 0)));

        expected = new double[]{1.003100395202637E+00, -7.517624567299208E-07, 2.523118658448453E-10};
        assertTrue(Arrays.equals(expected, lut.getCoefficients(14, 563)));

        expected = new double[]{9.990245699882507E-01, -1.933345998850200E-07, 1.901073187760005E-10};
        assertTrue(Arrays.equals(expected, lut.getCoefficients(14, 923)));

        expected = new double[]{9.997913837432861E-01, -4.594830826931684E-08, -3.999341829930003E-11};
        assertTrue(Arrays.equals(expected, lut.getCoefficients(14, 111)));
    }

}
