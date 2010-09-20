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

package org.esa.beam.equalization;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.*;

public class EqualizationLUTTest {

    @Test
    public void testGetCoefficientsReproTwoRR() throws IOException {
        final EqualizationLUT lut = new EqualizationLUT(2, false);

        double[] expected = {1.000257611274719E+00, 4.237312793975434E-07, -2.610836546956818E-10};
        compare(expected, lut.getCoefficients(0, 0));

        expected = new double[]{9.999011754989624E-01, -2.096630993264625E-07, 6.507311911541080E-10};
        compare(expected, lut.getCoefficients(0, 563));

        expected = new double[]{1.000722289085388E+00, 9.197131589644414E-08, 7.085015907293268E-11};
        compare(expected, lut.getCoefficients(0, 923));

        expected = new double[]{1.000316500663757E+00, -4.648632341286429E-07, 1.942934528542661E-11};
        compare(expected, lut.getCoefficients(0, 111));
    }

    @Test
    public void testGetCoefficientsReproThreeRR() throws IOException {
        final EqualizationLUT lut = new EqualizationLUT(3, false);

        double[] expected = {1.001474142074585E+00, -5.801677502859093E-07, 1.998069071307285E-10};
        compare(expected, lut.getCoefficients(14, 0));

        expected = new double[]{1.003061532974243E+00, -7.253045168909011E-07, 2.848663804844165E-10};
        compare(expected, lut.getCoefficients(14, 563));

        expected = new double[]{9.982740879058838E-01, 2.230187419627327E-06, -7.070350971360995E-10};
        compare(expected, lut.getCoefficients(14, 923));

        expected = new double[]{1.000137686729431E+00, -4.982850896340096E-07, 8.730229039688453E-11};
        compare(expected, lut.getCoefficients(14, 111));
    }

    @Test
    public void testGetCoefficientsReproTwoFR() throws IOException {
        final EqualizationLUT lut = new EqualizationLUT(2, true);

        double[] expected = {9.999982118606567E-01, 6.035877504473319E-07, -3.360867695256786E-10};
        compare(expected, lut.getCoefficients(0, 0));

        expected = new double[]{1.000030994415283E+00, 6.511432104616688E-08, -5.200747818512319E-11};
        compare(expected, lut.getCoefficients(0, 563));

        expected = new double[]{1.000398039817810E+00, 6.662139639956877E-07, -8.196916756464390E-11};
        compare(expected, lut.getCoefficients(0, 3698));

        expected = new double[]{1.000996947288513E+00, -2.004364603180875E-07, 2.039348204196934E-11};
        compare(expected, lut.getCoefficients(0, 14));
    }

    @Test
    public void testGetCoefficientsReproThreeFR() throws IOException {
        final EqualizationLUT lut = new EqualizationLUT(3, true);

        double[] expected = {1.001217365264893E+00, -4.410683800415427E-07, 9.581860999086089E-11};
        compare(expected, lut.getCoefficients(14, 0));

        expected = new double[]{9.994735121726990E-01, -5.710546702175634E-08, 3.497988010359165E-11};
        compare(expected, lut.getCoefficients(14, 2205));

        expected = new double[]{9.979037642478943E-01, 3.237862301830319E-06, -1.023005680167444E-09};
        compare(expected, lut.getCoefficients(14, 3698));

        expected = new double[]{1.001017928123474E+00, -2.546333462305483E-06, 9.838577730292286E-10};
        compare(expected, lut.getCoefficients(14, 314));
    }

    private void compare(double[] expected, double[] actualCoefficients) {
        assertEquals(expected[0], actualCoefficients[0]);
        assertEquals(expected[1], actualCoefficients[1]);
        assertEquals(expected[2], actualCoefficients[2]);
    }

}
