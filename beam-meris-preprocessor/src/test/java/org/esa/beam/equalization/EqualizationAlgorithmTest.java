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

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.MathUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static junit.framework.Assert.*;

public class EqualizationAlgorithmTest {

    private static final double[] EXPECTED_RR_REPRO2_VALUES = new double[]{
            23.3939, 23.4582, 23.3923, 23.4541,
            120.1749, 120.5050, 120.1665, 120.4841
    };
    private static final double[] EXPECTED_RR_REPRO3_VALUES = new double[]{
            23.4242, 23.4401, 23.4179, 23.4286,
            120.3305, 120.4123, 120.2979, 120.3532
    };
    private static final double[] EXPECTED_FR_REPRO2_VALUES = new double[]{
            23.4618, 23.4465, 23.4424, 23.4466,
            120.5233, 120.4450, 120.4237, 120.4455
    };
    private static final double[] EXPECTED_FR_REPRO3_VALUES = new double[]{
            23.4392, 23.4261, 23.4366, 23.4290,
            120.4075, 120.3400, 120.3941, 120.3550
    };

    @Test
    public void testPerformEqualization() throws Exception {

        ProductData.UTC utc = ProductData.UTC.create(new Date(), 0);
        checkValues(EXPECTED_RR_REPRO2_VALUES, generateActualValues(new EqualizationAlgorithm(2, false, utc)));
        checkValues(EXPECTED_RR_REPRO3_VALUES, generateActualValues(new EqualizationAlgorithm(3, false, utc)));
        checkValues(EXPECTED_FR_REPRO2_VALUES, generateActualValues(new EqualizationAlgorithm(2, true, utc)));
        checkValues(EXPECTED_FR_REPRO3_VALUES, generateActualValues(new EqualizationAlgorithm(3, true, utc)));
    }

    @Test()
    public void testParseReproVersion_Fails() throws Exception {
        try {
            EqualizationAlgorithm.detectReprocessingVersion("MERIS", "3.67");
            fail("Version is not of reprocessing 2 or 3");
        } catch (Exception ignored) {
            // expected
        }
        try {
            EqualizationAlgorithm.detectReprocessingVersion("MERIS", "5.1");
            fail("Version is not of reprocessing 2 or 3");
        } catch (Exception ignored) {
            // expected
        }

        try {
            EqualizationAlgorithm.detectReprocessingVersion("MEGS-PC", "7.2");
            fail("Version is not of reprocessing 2 or 3");
        } catch (Exception ignored) {
            // expected
        }
    }

    @Test
    public void testParseReprocessingVersion() throws Exception {
        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MERIS", "4.1"));
        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MERIS", "5.02"));
        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MERIS", "5.03"));
        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MERIS", "5.04"));
        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MERIS", "5.05"));
        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MERIS", "5.06"));

        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MEGS-PC", "7.4"));
        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MEGS-PC", "7.4.1"));
        assertEquals(2, EqualizationAlgorithm.detectReprocessingVersion("MEGS-PC", "7.5"));

        assertEquals(3, EqualizationAlgorithm.detectReprocessingVersion("MEGS-PC", "8.0"));
        assertEquals(3, EqualizationAlgorithm.detectReprocessingVersion("MEGS-PC", "8.1"));
        assertEquals(3, EqualizationAlgorithm.detectReprocessingVersion("MEGS-PC", "8.9"));
    }

    @Test
    public void testVersionToFloat() throws Exception {
        assertEquals(4.1f, EqualizationAlgorithm.versionToFloat("4.1   "), 0.0f);
        assertEquals(4.12f, EqualizationAlgorithm.versionToFloat("  4.1.2"), 0.0f);
        assertEquals(8.6f, EqualizationAlgorithm.versionToFloat("8.6 "), 0.0f);
        assertEquals(1234.0f, EqualizationAlgorithm.versionToFloat("1234"), 0.0f);
    }

    @Test
    public void testToJulianDay() {
        assertEquals(2455414, EqualizationAlgorithm.toJulianDay(2010, 7, 6));
        assertEquals(2452365, EqualizationAlgorithm.toJulianDay(2002, 3, 1));
    }

    private void checkValues(double[] expectedValues, double[] actualValues) {
        for (int i = 0; i < expectedValues.length; i++) {
            double expected = expectedValues[i];
            final double actual = actualValues[i];
            if (!MathUtils.equalValues(expected, actual, 1.0e-3)) {
                fail(String.format("Error at index: %d\nexpected:<%s> but was: <%s>", i,
                                   Arrays.toString(expectedValues), Arrays.toString(actualValues)));
            }
        }
    }

    private double[] generateActualValues(EqualizationAlgorithm equalizationAlgorithm) {
        final double[] doubles = new double[8];

        doubles[0] = equalizationAlgorithm.performEqualization(23.43, 2, 325);
        doubles[1] = equalizationAlgorithm.performEqualization(23.43, 2, 489);
        doubles[2] = equalizationAlgorithm.performEqualization(23.43, 3, 325);
        doubles[3] = equalizationAlgorithm.performEqualization(23.43, 3, 489);
        doubles[4] = equalizationAlgorithm.performEqualization(120.36, 2, 325);
        doubles[5] = equalizationAlgorithm.performEqualization(120.36, 2, 489);
        doubles[6] = equalizationAlgorithm.performEqualization(120.36, 3, 325);
        doubles[7] = equalizationAlgorithm.performEqualization(120.36, 3, 489);
        return doubles;
    }

}
