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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * EnvisatAuxReader Tester.
 *
 * @author lveci
 */
public class EnvisatAuxReaderTest {

    String ers1XCAFilePath = "org/esa/snap/dataio/testdata/ER1_XCA_AXNXXX20050321_000000_19910101_000000_20100101_000000.txt";
    String ers2XCAFilePath = "org/esa/snap/dataio/testdata/ER2_XCA_AXNXXX20050321_000000_19950101_000000_20100101_000000.txt";
    String envisatXCAFilePath = "org/esa/snap/dataio/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000";
    String envisatXCAZipFilePath = "org/esa/snap/dataio/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000.zip";
    String envisatXCAZipFilePath2 = "org/esa/snap/dataio/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000_2";
    String envisatXCAGZFilePath = "org/esa/snap/dataio/testdata/ASA_XCA_AXVIEC20070517_153558_20070204_165113_20071231_000000.gz";

    @Test
    public void testAutoLookupZIP() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(envisatXCAZipFilePath2);
        testAuxDataFromGADS(reader);
    }

    @Test
    public void testUncompressed() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(envisatXCAFilePath);
        testAuxDataFromGADS(reader);
    }

    @Test
    public void testERS1() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(ers1XCAFilePath);
    }

    @Test
    public void testERS2() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(ers2XCAFilePath);
    }

    @Test
    public void testGZIP() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(envisatXCAGZFilePath);
        testAuxDataFromGADS(reader);
    }

    @Test
    public void testZIP() throws IOException {
        EnvisatAuxReader reader = new EnvisatAuxReader();
        reader.readProduct(envisatXCAZipFilePath);
        testAuxDataFromGADS(reader);
    }

    static void testAuxDataFromGADS(EnvisatAuxReader reader) throws ProductIOException {
        ProductData extCalImVvData = reader.getAuxData("ext_cal_im_vv");
        assertNotNull(extCalImVvData);

        final float[] extCalImVv = ((float[]) extCalImVvData.getElems());

        assertEquals(7, extCalImVv.length);
        assertEquals(34994.515625f, extCalImVv[0], 1e-6f);
        assertEquals(32284.941406f, extCalImVv[1], 1e-5f);
        assertEquals(39084.089843f, extCalImVv[2], 1e-5f);
        assertEquals(33113.109375f, extCalImVv[3], 1e-5f);
        assertEquals(34994.516000f, extCalImVv[4], 1e-5f);
        assertEquals(34994.516000f, extCalImVv[5], 1e-5f);

        ProductData elevAngleData = reader.getAuxData("elev_ang_is1");
        float elevAngle1 = elevAngleData.getElemFloat();
        assertEquals(16.628, elevAngle1, 1e-5);

        ProductData patData = reader.getAuxData("pattern_is1");
        final float[] pattern1 = ((float[]) patData.getElems());

        assertEquals(804, pattern1.length);
        assertEquals(-18.6224f, pattern1[0], 1e-5f);
        assertEquals(-17.4271f, pattern1[1], 1e-5f);
        assertEquals(-16.3024f, pattern1[2], 1e-5f);
        assertEquals(-18.7799f, pattern1[804-3], 1e-5f);
        assertEquals(-19.5464f, pattern1[804-2], 1e-5f);
        assertEquals(-20.3164f, pattern1[804-1], 1e-5f);

        assertEquals("-18.6224", patData.getElemStringAt(0));
    }

}
