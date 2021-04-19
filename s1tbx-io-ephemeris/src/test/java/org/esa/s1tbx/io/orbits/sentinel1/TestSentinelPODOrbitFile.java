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

        // First OSV (exact match)
        String utcStr1 = "UTC=2014-05-25T15:19:21.698661";
        double utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        Orbits.OrbitVector orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -3.1501508247974257E19);
        assert(orbitData.yPos == 8.268709303811858E19);
        assert(orbitData.zPos == 2.4697297126022783E19);
        assert(orbitData.xVel == 2.9129073756671728E16);
        assert(orbitData.yVel == 3.6850695790661096E16);
        assert(orbitData.zVel == -8.8394902904799616E16);

        // Last OSV (exact match)
        utcStr1 = "UTC=2014-05-25T18:36:41.698661";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -3.147332110679456E19);
        assert(orbitData.yPos == 8.26131072549093E19);
        assert(orbitData.zPos == 2.467519679832084E19);
        assert(orbitData.xVel == 2.9103010465610332E16);
        assert(orbitData.yVel == 3.6817720802569768E16);
        assert(orbitData.zVel == -8.8315809808721968E16);

        // 500th OSV (exact match)
        utcStr1 = "UTC=2014-05-25T16:42:31.698661";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -3.1489626650287845E19);
        assert(orbitData.yPos == 8.265590614684035E19);
        assert(orbitData.zPos == 2.4687981275085324E19);
        assert(orbitData.xVel == 2.911808741596072E16);
        assert(orbitData.yVel == 3.6836795993490472E16);
        assert(orbitData.zVel == -8.8361563151106112E16);

        // between 450th and 451th OSV (closer to 450th)
        utcStr1 = "UTC=2014-05-25T16:34:14.698661";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -3.1490809913905762E19);
        assert(orbitData.yPos == 8.265901198452882E19);
        assert(orbitData.zPos == 2.4688909021310206E19);
        assert(orbitData.xVel == 2.9119181522802116E16);
        assert(orbitData.yVel == 3.683818024539328E16);
        assert(orbitData.zVel == -8.8364883387887632E16);

        // between 450th and 451th OSV (closer to 451th)
        utcStr1 = "UTC=2014-05-25T16:34:18.698661";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -3.149080039053903E19);
        assert(orbitData.yPos == 8.265898698753671E19);
        assert(orbitData.zPos == 2.468890155444702E19);
        assert(orbitData.xVel == 2.9119172717003868E16);
        assert(orbitData.yVel == 3.6838169104394488E16);
        assert(orbitData.zVel == -8.8364856665328064E16);

        // OSV earlier than all
        utcStr1 = "UTC=2014-05-25T15:10:21.698661";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -3.1502794211280278E19);
        assert(orbitData.yPos == 8.269046844256785E19);
        assert(orbitData.zPos == 2.4698305394683273E19);
        assert(orbitData.xVel == 2.913026282495338E16);
        assert(orbitData.yVel == 3.6852200186762112E16);
        assert(orbitData.zVel == -8.8398511316807696E16);

        // OSV later than all
        utcStr1 = "UTC=2014-05-25T18:36:41.699662";
        utc1 = Sentinel1OrbitFileReader.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -3.147332110441233E19);
        assert(orbitData.yPos == 8.26131072486564E19);
        assert(orbitData.zPos == 2.467519679645303E19);
        assert(orbitData.xVel == 2.9103010463407592E16);
        assert(orbitData.yVel == 3.6817720799782896E16);
        assert(orbitData.zVel == -8.8315809802037424E16);


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
