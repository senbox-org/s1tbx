/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.geometric;

import org.esa.s1tbx.S1TBXTests;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.gpf.TestProcessor;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

/**
 * Unit test for EllipsoidCorrectionRDOp.
 */
public class TestEllipsoidCorrectionRDOp {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new EllipsoidCorrectionRDOp.Spi();
    private final static TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "GeoTIFF", "DOR_VOR_AX"};
    private String[] exceptionExemptions = {"not supported", "not be map projected",
            "Source product should first be deburst"};

    @Test
    public void testProcessAllASAR() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsASAR, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllERS() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsERS, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllALOS() throws Exception
    {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsALOS, "ALOS PALSAR CEOS", null, exceptionExemptions);
    }

    @Test
    public void testProcessAllRadarsat2() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsRadarsat2, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllTerraSARX() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsTerraSarX, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllCosmo() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsCosmoSkymed, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllSentinel1() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsSentinel1, null, exceptionExemptions);
    }
}
