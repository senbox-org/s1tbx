/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.orbits.sentinel1;

import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * To test SentinelPODOrbitFile
 */
public class TestSentinelPODOrbitFile {

    private final static File orbitFile = new File(TestData.inputSAR+"Orbits"+TestData.sep+
            "S1A_OPER_AUX_RESORB_OPOD_20140611T152302_V20140525T151921_20140525T183641.EOF");

    final File sourceFile = TestData.inputS1_GRDSubset;

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(orbitFile + "not found", orbitFile.exists());
        assumeTrue(sourceFile + "not found", sourceFile.exists());
    }

    @Test
    public void testRetrieveOrbitFile() throws Exception {
        final File sourceFile = TestData.inputS1_GRDSubset;

        final Product sourceProduct = ProductIO.readProduct(sourceFile);
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        final SentinelPODOrbitFile podOrbitFile = new SentinelPODOrbitFile(absRoot, 3);
        podOrbitFile.retrieveOrbitFile(SentinelPODOrbitFile.PRECISE);
    }

    @Test
    public void testSentinelPODOrbitFile() throws Exception {

        final Product sourceProduct = ProductIO.readProduct(sourceFile);
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        final SentinelPODOrbitFile podOrbitFile = new SentinelPODOrbitFile(absRoot, 3);
        podOrbitFile.retrieveOrbitFile(SentinelPODOrbitFile.PRECISE);

        assertEquals("1.10.1", podOrbitFile.getVersion());

        // First OSV (exact match)
        String utcStr1 = "UTC=2015-08-27T22:59:43.000000";
        double utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        Orbits.OrbitVector orbitData = podOrbitFile.getOrbitData(utc1);
        assertEquals(-368251.718419, orbitData.xPos, 0.00001);
        assertEquals(-2299963.236657, orbitData.yPos, 0.00001);
        assertEquals(6671242.884855, orbitData.zPos, 0.00001);
        assertEquals(-2359.280562, orbitData.xVel, 0.00001);
        assertEquals(6854.762675, orbitData.yVel, 0.00001);
        assertEquals(2228.116584, orbitData.zVel, 0.00001);

        // Last OSV (exact match)
        utcStr1 = "UTC=2015-08-29T00:59:43.000000";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assertEquals(-1801672.0416870117, orbitData.xPos, 0.00001);
        assertEquals(-6843683.1560668945, orbitData.yPos, 0.00001);
        assertEquals(-10341.53857421875, orbitData.zPos, 0.00001);
        assertEquals(-1531.6392338871956, orbitData.xVel, 0.00001);
        assertEquals(401.30609583854675, orbitData.yVel, 0.00001);
        assertEquals(7430.427426996641, orbitData.zVel, 0.00001);

        // 500th OSV (exact match)
        utcStr1 = "UTC=2015-08-28T00:23:03.000000";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assertEquals(-942678.5061780214, orbitData.xPos, 0.00001);
        assertEquals(-6731873.704930037, orbitData.yPos, 0.00001);
        assertEquals(1959410.9963270426, orbitData.zPos, 0.00001);
        assertEquals(-1304.736001000012, orbitData.xVel, 0.00001);
        assertEquals(2269.5797080000048, orbitData.yVel, 0.00001);
        assertEquals(7132.566981000011, orbitData.zVel, 0.00001);

        // between 450th and 451th OSV (closer to 450th)
        utcStr1 = "UTC=2015-08-28T00:14:45.000000";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assertEquals(-178042.07025708258, orbitData.xPos, 0.00001);
        assertEquals(-6869995.854708411, orbitData.yPos, 0.00001);
        assertEquals(-1697201.8589743972, orbitData.zPos, 0.00001);
        assertEquals(-1645.6544726879947, orbitData.xVel, 0.00001);
        assertEquals(-1730.7971334199392, orbitData.yVel, 0.00001);
        assertEquals(7208.367487452444, orbitData.zVel, 0.00001);

        // between 450th and 451th OSV (closer to 451th)
        utcStr1 = "UTC=2015-08-28T00:14:50.999999";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assertEquals(-187916.8427630216, orbitData.xPos, 0.00001);
        assertEquals(-6880237.810813792, orbitData.yPos, 0.00001);
        assertEquals(-1653917.5010708272, orbitData.zPos, 0.00001);
        assertEquals(-1645.9188845989265, orbitData.xVel, 0.00001);
        assertEquals(-1683.1769871794822, orbitData.yVel, 0.00001);
        assertEquals(7219.705373079771, orbitData.zVel, 0.00001);

        // OSV earlier than all
        utcStr1 = "UTC=2014-05-25T15:10:21.698661";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assertEquals(-3.150273170852699E19, orbitData.xPos, 0.00001);
        assertEquals(8.269037468208297E19, orbitData.yPos, 0.00001);
        assertEquals(2.4698065798492193E19, orbitData.zPos, 0.00001);
        assertEquals(2.9099011052782536E16, orbitData.xVel, 0.00001);
        assertEquals(3.6873034709899288E16, orbitData.yVel, 0.00001);
        assertEquals(-8.8408928573603792E16, orbitData.zVel, 0.00001);

        // OSV later than all
        utcStr1 = "UTC=2015-09-10T18:36:41.699662";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assertEquals(6.475599032734154E14, orbitData.xPos, 0.00001);
        assertEquals(-1.2459061082351416E14, orbitData.yPos, 0.00001);
        assertEquals(-1.8615578366160958E15, orbitData.zPos, 0.00001);
        assertEquals(-5.251709936239622E11, orbitData.xVel, 0.00001);
        assertEquals(-2.0816778799893662E12, orbitData.yVel, 0.00001);
        assertEquals(-5.457739965786061E10, orbitData.zVel, 0.00001);


        final Sentinel1OrbitFileReader orbitFileReader = new Sentinel1OrbitFileReader(podOrbitFile.getOrbitFile());
        orbitFileReader.read();

        String str = orbitFileReader.getMissionFromHeader();
        if(str != null) {
            TestUtils.log.info("Mission from Header = " + str);
            assert (str.startsWith("Sentinel-1"));
        }

        str = orbitFileReader.getFileTypeFromHeader();
        if(str != null) {
            TestUtils.log.info("File_Type from Header = " + str);
            assertEquals("AUX_POEORB", str);
        }

        str = orbitFileReader.getValidityStartFromHeader();
        if(str != null) {
            TestUtils.log.info("Validity_Start from Header = " + str);
            assertEquals("UTC=2015-08-27T22:59:43", str);
        }

        str = orbitFileReader.getValidityStopFromHeader();
        if(str != null) {
            TestUtils.log.info("Validity_Stop from Header = " + str);
            assertEquals("UTC=2015-08-29T00:59:43", str);
        }
    }

    @Test
    public void testSentinelPODOrbitValidity() {
        final String vStart = Sentinel1OrbitFileReader.getValidityStartFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        TestUtils.log.info("validity start from filename = " + vStart );
        assertEquals("UTC=2014-05-09T22:59:44", vStart);

        final String vStop = Sentinel1OrbitFileReader.getValidityStopFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        TestUtils.log.info("validity stop from filename = " + vStop);
        assertEquals("UTC=2014-05-11T00:59:44", vStop);
    }

    @Test
    public void testSentinelPODOrbitFileOperations() throws Exception {

        String name = orbitFile.getName();
        final ProductData.UTC utcStart = Sentinel1OrbitFileReader.getValidityStartFromFilenameUTC(name);
        assertEquals(5258.6384375, utcStart.getMJD(), 0.00001);

        final ProductData.UTC utcEnd = Sentinel1OrbitFileReader.getValidityStopFromFilenameUTC(name);
        assertEquals(5258.775474537037, utcEnd.getMJD(), 0.00001);
    }
}
