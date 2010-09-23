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

package org.esa.beam.preprocessor.equalization;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Date;

import static junit.framework.Assert.*;

public class EqualizationAlgorithmTest {

    @Test
    public void testPerformEqualization() throws Exception {

        ProductData.UTC utc = ProductData.UTC.create(new Date(), 0);
        Reader[] readers = new Reader[]{
                new StringReader("1.0 2.0 3.0\n4.0 5.0 6.0"),
                new StringReader("0.1 0.2 0.3\n0.4 0.5 0.6")
        };

        EqualizationAlgorithm algorithm = new EqualizationAlgorithm(utc, new EqualizationLUT(readers));
        long date = algorithm.getJulianDate();
        double sample = 100.0;
        final long squareDate = date * date;
        assertEquals(sample / (1.0 + 2.0 * date + 3.0 * squareDate),
                     algorithm.performEqualization(sample, 0, 0), 1.0e-6);
        assertEquals(sample / (4.0 + 5.0 * date + 6.0 * squareDate),
                     algorithm.performEqualization(sample, 0, 1), 1.0e-6);
        assertEquals(sample / (0.1 + 0.2 * date + 0.3 * squareDate),
                     algorithm.performEqualization(sample, 1, 0), 1.0e-6);
        assertEquals(sample / (0.4 + 0.5 * date + 0.6 * squareDate),
                     algorithm.performEqualization(sample, 1, 1), 1.0e-6);
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
    public void testGetJulianDate() throws ParseException, IOException {
        ProductData.UTC utc = ProductData.UTC.parse("12-10-2006", "dd-MM-yyyy");
        EqualizationAlgorithm algorithm = new EqualizationAlgorithm(utc, new EqualizationLUT(new Reader[0]));
        long expectedJD = EqualizationAlgorithm.toJulianDay(2006, 9, 12) - EqualizationAlgorithm.toJulianDay(2002, 4,
                                                                                                             1);
        assertEquals(expectedJD, algorithm.getJulianDate());
    }

    @Test
    public void testToJulianDay() {
        assertEquals(2455414, EqualizationAlgorithm.toJulianDay(2010, 7, 6));
        assertEquals(2452365, EqualizationAlgorithm.toJulianDay(2002, 3, 1));
    }

}
