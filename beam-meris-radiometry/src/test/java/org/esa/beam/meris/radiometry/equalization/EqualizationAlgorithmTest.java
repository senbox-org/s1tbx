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

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Date;

import static junit.framework.Assert.*;
import static org.esa.beam.meris.radiometry.equalization.EqualizationAlgorithm.*;

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

//        // RR
//        MER_RAC_AXVIEC20050606_071652_20021224_121445_20041213_220000    // before Repro2
//        MER_RAC_AXVIEC20050607_071652_20021224_121445_20041213_220000    // first Repro2
//        MER_RAC_AXVACR20091007_171024_20061009_220000_20161009_220000    // last Repro2
//        MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000    // first Repro3
//
//        // FR
//        MER_RAC_AXVIEC20050707_135806_20041213_220000_20141213_220000    // before Repro2
//        MER_RAC_AXVIEC20050708_135806_20041213_220000_20141213_220000    // first Repro2
//        MER_RAC_AXVACR20091007_171024_20061009_220000_20161009_220000    // last Repro2
//        MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000    // first Repro3

    @Test
    public void testParseReprocessingVersion_RR() {
        assertEquals(-1, detectReprocessingVersion("MER_RAC_AXVIEC20050606_071652_20021224_121445_20041213_220000",
                                                   true));
        assertEquals(2, detectReprocessingVersion("MER_RAC_AXVIEC20050607_071652_20021224_121445_20041213_220000",
                                                  true));
        assertEquals(2, detectReprocessingVersion("MER_RAC_AXVIEC20070302", true));
        assertEquals(2, detectReprocessingVersion("MER_RAC_AXVACR20091007_171024_20061009_220000_20161009_220000",
                                                  true));
        assertEquals(3, detectReprocessingVersion("MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000",
                                                  true));
        assertEquals(3, detectReprocessingVersion("MER_RAC_AXVACR20111008",
                                                  true));
    }

    @Test
    public void testParseReprocessingVersion_FR() {
        assertEquals(-1, detectReprocessingVersion("MER_RAC_AXVIEC20050707_135806_20041213_220000_20141213_220000",
                                                   false));
        assertEquals(2, detectReprocessingVersion("MER_RAC_AXVIEC20050708_135806_20041213_220000_20141213_220000",
                                                  false));
        assertEquals(2, detectReprocessingVersion("MER_RAC_AXVIEC20070302", false));
        assertEquals(2, detectReprocessingVersion("MER_RAC_AXVACR20091007_171024_20061009_220000_20161009_220000",
                                                  false));
        assertEquals(3, detectReprocessingVersion("MER_RAC_AXVACR20091008_171024_20061009_220000_20161009_220000",
                                                  false));
        assertEquals(3, detectReprocessingVersion("MER_RAC_AXVACR20111008",
                                                  false));
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
