/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.TestNotExecutableException;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.ProductData;

public class DDDBTest extends TestCase {

    private DDDB _dddb;

    public DDDBTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        if (DDDB.isInstalled()) {
            return new TestSuite(DDDBTest.class);
        } else {
            return new TestCase(DDDBTest.class.getName()) {

                @Override
                public void runTest() {
                    System.out.println();
                    System.out.println(DDDBTest.class + ": warning: test will not be performed: DDDB not installed: ");
                    System.out.println(DDDB.DB_DIR_PATH);
                }
            };
        }
    }

    @Override
    protected void setUp() {
        _dddb = DDDB.getInstance();
    }

    @Override
    protected void tearDown() {
        _dddb = null;
    }

    public void testGetDatasetNames() {
        try {
            assertEquals("Radiance_9", _dddb.getDatasetNames("MER_RR__1P")[11]);
            assertEquals("Radiance_9", _dddb.getDatasetNames("MER_FR__1P")[11]);
            assertEquals("Chl_1_TOAVI_CTP", _dddb.getDatasetNames("MER_RR__2P")[17]);
            assertEquals("Chl_1_TOAVI_CTP", _dddb.getDatasetNames("MER_FR__2P")[17]);
            assertEquals("Cloud_Type_OT", _dddb.getDatasetNames("MER_LRC_2P")[3]);
            assertEquals("Vapour_Content", _dddb.getDatasetNames("MER_RRC_2P")[5]);
            assertEquals("BOAVI", _dddb.getDatasetNames("MER_RRV_2P")[4]);
            assertEquals("VISIBLE_CALIB_COEFS_GADS", _dddb.getDatasetNames("ATS_TOA_1P")[5]);
            assertEquals("DISTRIB_SST_CLOUD_LAND_MDS", _dddb.getDatasetNames("ATS_NR__2P")[7]);
            try {
                _dddb.getDatasetNames("not a product");
                fail("DDDBException expected");
            } catch (DDDBException e) {
            }
            try {
                _dddb.getDatasetNames(null);
                fail("IllegalArgumentException expected");
            } catch (IllegalArgumentException e) {
            }
        } catch (DDDBException e) {
            throw new TestNotExecutableException(e);
        }
    }

    public void testReadRecordInfo() {
        try {
            java.util.Hashtable params = new java.util.Hashtable();

            assertNotNull(_dddb.readRecordInfo("MER_RR__1P", "Quality_ADS", null));
            assertNotNull(_dddb.readRecordInfo("MER_RR__1P", "QUALITY_ADS", null));
            assertNotNull(_dddb.readRecordInfo("MER_RR__1P", "Scaling_Factor_GADS", null));
            assertNotNull(_dddb.readRecordInfo("MER_RR__1P", "SCALING_FACTOR_GADS", null));

            params.put("tiePointGridWidth", new Integer(71));
            assertNotNull(_dddb.readRecordInfo("MER_RR__1P", "Tie_points_ADS", params));
            assertNotNull(_dddb.readRecordInfo("MER_RR__1P", "TIE_POINTS_ADS", params));

            try {
                _dddb.readRecordInfo("MER_RR__1P", "not a dataset", null);
                fail("DDDBException expected");
            } catch (DDDBException e) {
            }

            try {
                _dddb.readRecordInfo("not a product", "not a dataset", null);
                fail("DDDBException expected");
            } catch (DDDBException e) {
            }

            try {
                _dddb.readRecordInfo("MER_RR__1P", null, null);
                fail("IllegalArgumentException expected");
            } catch (IllegalArgumentException e) {
            }

            try {
                _dddb.readRecordInfo(null, "not a dataset", null);
                fail("IllegalArgumentException expected");
            } catch (IllegalArgumentException e) {
            }
        } catch (DDDBException e) {
            throw new TestNotExecutableException(e);
        }
    }

    public void testDataIntegrity() {
        try {
            java.util.Hashtable params = new java.util.Hashtable();

            params.put("tiePointGridWidth", new Integer(71));
            params.put("sceneRasterWidth", new Integer(1121));

            testDataBaseIntegrity("MER_RR__1P", "Quality_ADS", params, 4, 33);
            testDataBaseIntegrity("MER_RR__1P", "Scaling_Factor_GADS", params, 12, 292);
            testDataBaseIntegrity("MER_RR__1P", "Tie_points_ADS", params, 17, 3563);
            testDataBaseIntegrity("MER_RR__1P", "Radiance_9", params, 3, 2255);
            testDataBaseIntegrity("MER_RR__1P", "Flags", params, 4, 3376);

            params.put("tiePointGridWidth", new Integer(36));
            params.put("sceneRasterWidth", new Integer(2241));

            testDataBaseIntegrity("MER_FR__1P", "Quality_ADS", params, 4, 33);
            testDataBaseIntegrity("MER_FR__1P", "Scaling_Factor_GADS", params, 12, 292);
            testDataBaseIntegrity("MER_FR__1P", "Tie_points_ADS", params, 17, 1813);
            testDataBaseIntegrity("MER_FR__1P", "Radiance_9", params, 3, 4495);
            testDataBaseIntegrity("MER_FR__1P", "Flags", params, 4, 6736);

        } catch (DDDBException e) {
            throw new TestNotExecutableException(e);
        }
    }

    private void testDataBaseIntegrity(String productType,
                                       String datasetName,
                                       java.util.Map params,
                                       int numFieldsExpected,
                                       int sizeInBytesExpected) throws DDDBException {
        RecordInfo info = _dddb.readRecordInfo(productType, datasetName, params);
        String msg = "prd='" + productType + "',ds='" + datasetName + "',params=" + params;
        assertNotNull(msg, info);
        assertEquals(msg, numFieldsExpected, info.getNumFieldInfos());
        assertEquals(msg, sizeInBytesExpected, info.getSizeInBytes());
    }


    public void testThatAllFieldTypesAreRecognized() {
        assertEquals(ProductData.TYPE_INT8, DDDB.getFieldType("SChar"));
        assertEquals(ProductData.TYPE_UINT8, DDDB.getFieldType("UChar"));
        assertEquals(ProductData.TYPE_INT16, DDDB.getFieldType("SShort"));
        assertEquals(ProductData.TYPE_UINT16, DDDB.getFieldType("UShort"));
        assertEquals(ProductData.TYPE_INT32, DDDB.getFieldType("SLong"));
        assertEquals(ProductData.TYPE_UINT32, DDDB.getFieldType("ULong"));
        assertEquals(ProductData.TYPE_FLOAT32, DDDB.getFieldType("Float"));
        assertEquals(ProductData.TYPE_FLOAT64, DDDB.getFieldType("Double"));
        assertEquals(ProductData.TYPE_ASCII, DDDB.getFieldType("String"));
        assertEquals(ProductData.TYPE_UTC, DDDB.getFieldType("@/types/UTC.dd"));

        assertEquals(ProductData.TYPE_ASCII, DDDB.getFieldType("string"));
        assertEquals(ProductData.TYPE_ASCII, DDDB.getFieldType("STRING"));
        assertEquals(ProductData.TYPE_UNDEFINED, DDDB.getFieldType("String "));

        try {
            DDDB.getFieldType(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        assertEquals("SChar", DDDB.getFieldTypeName(ProductData.TYPE_INT8));
        assertEquals("UChar", DDDB.getFieldTypeName(ProductData.TYPE_UINT8));
        assertEquals("SShort", DDDB.getFieldTypeName(ProductData.TYPE_INT16));
        assertEquals("UShort", DDDB.getFieldTypeName(ProductData.TYPE_UINT16));
        assertEquals("SLong", DDDB.getFieldTypeName(ProductData.TYPE_INT32));
        assertEquals("ULong", DDDB.getFieldTypeName(ProductData.TYPE_UINT32));
        assertEquals("Float", DDDB.getFieldTypeName(ProductData.TYPE_FLOAT32));
        assertEquals("Double", DDDB.getFieldTypeName(ProductData.TYPE_FLOAT64));
        assertEquals("String", DDDB.getFieldTypeName(ProductData.TYPE_ASCII));
        assertEquals("@/types/UTC.dd", DDDB.getFieldTypeName(ProductData.TYPE_UTC));
        assertEquals("?", DDDB.getFieldTypeName(ProductData.TYPE_UNDEFINED));
        try {
            DDDB.getFieldTypeName(32985);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetInstance() {
        assertNotNull(DDDB.getInstance());
        assertSame(_dddb, DDDB.getInstance());
    }

    public void testMERIS_L2_Flags_IODD7() {
        FlagCoding flagCoding = _dddb.readFlagsCoding("l2_flags", "flags/MER_RR__2P_flags_IODD7.dd");

        assertNotNull(flagCoding);
        assertEquals(0, flagCoding.getNumElements());
        assertEquals(31, flagCoding.getNumAttributes());

        MetadataAttribute[] attributes = flagCoding.getAttributes();

        String[] expectedNames = new String[]{
                "LAND",
                "CLOUD",
                "WATER",
                "PCD_1_13",
                "PCD_14",
                "PCD_15",
                "PCD_16",
                "PCD_17",
                "PCD_18",
                "PCD_19",
                "COASTLINE",
                "COSMETIC",
                "SUSPECT",
                "OOADB",
                "ABSOA_DUST",
                "CASE2_S",
                "CASE2_ANOM",
                "TOAVI_BRIGHT",
                "CASE2_Y",
                "TOAVI_BAD",
                "ICE_HAZE",
                "TOAVI_CSI",
                "MEDIUM_GLINT",
                "TOAVI_WS",
                "LARS_ON",
                "BPAC_ON",
                "HIGH_GLINT",
                "TOAVI_INVAL_REC",
                "LOW_SUN",
                "LOW_PRESSURE",
                "WHITE_SCATTERER",
        };

        int[] expectedValues = new int[]{
                0x800000, // 0
                0x400000, // 1
                0x200000, // 2
                0x100000, // 3
                0x080000, // 4
                0x040000, // 5
                0x020000, // 6
                0x010000, // 7
                0x008000, // 8
                0x004000, // 9
                0x002000, // 10
                0x001000, // 11
                0x000800, // 12
                0x000400, // 13
                0x000200, // 14
                0x200100, // 15
                0x200080, // 16
                0x800080, // 17
                0x200040, // 18
                0x800040, // 19
                0x200020, // 20
                0x800020, // 21
                0x200010, // 22
                0x800010, // 23
                0x800008, // 24
                0x200008, // 25
                0x200004, // 26
                0x800004, // 27
                0x000002, // 28
                0x000001, // 29
                0x200001, // 30
        };
        for (int i = 0; i < attributes.length; i++) {
            MetadataAttribute attribute = attributes[i];
            String s = "at index [" + i + "]";
            assertNotNull(s, attribute);
            assertEquals(s, expectedNames[i], attribute.getName());
            assertEquals(s, 1, attribute.getNumDataElems());
            assertEquals(s, expectedValues[i], attribute.getData().getElemInt());
        }
    }

    public void testMERIS_L2_Flags() {
        FlagCoding flagCoding = _dddb.readFlagsCoding("l2_flags", "flags/MER_RR__2P_flags.dd");

        assertNotNull(flagCoding);
        assertEquals(0, flagCoding.getNumElements());
        assertEquals(31, flagCoding.getNumAttributes());

        MetadataAttribute[] attributes = flagCoding.getAttributes();

        String[] expectedNames = new String[]{
                "LAND",
                "CLOUD",
                "WATER",
                "PCD_1_13",
                "PCD_14",
                "PCD_15",
                "PCD_16",
                "PCD_17",
                "PCD_18",
                "PCD_19",
                "COASTLINE",
                "COSMETIC",
                "SUSPECT",
                "OOADB",
                "ABSOA_DUST",
                "CASE2_S",
                "SNOW_ICE",
                "CASE2_ANOM",
                "TOAVI_BRIGHT",
                "CASE2_Y",
                "TOAVI_BAD",
                "ICE_HAZE",
                "TOAVI_CSI",
                "MEDIUM_GLINT",
                "TOAVI_WS",
                "DDV",
                "BPAC_ON",
                "HIGH_GLINT",
                "TOAVI_INVAL_REC",
                "LOW_SUN",
                "WHITE_SCATTERER",
        };

        int[] expectedValues = new int[]{
                0x800000, // 0
                0x400000, // 1
                0x200000, // 2
                0x100000, // 3
                0x080000, // 4
                0x040000, // 5
                0x020000, // 6
                0x010000, // 7
                0x008000, // 8
                0x004000, // 9
                0x002000, // 10
                0x001000, // 11
                0x000800, // 12
                0x000400, // 13
                0x000200, // 14
                0x200100, // 15
                0x800100, // 16
                0x200080, // 17
                0x800080, // 18
                0x200040, // 19
                0x800040, // 20
                0x200020, // 21
                0x800020, // 22
                0x200010, // 23
                0x800010, // 24
                0x800008, // 25
                0x200008, // 26
                0x200004, // 27
                0x800004, // 28
                0x000002, // 29
                0x200001, // 30
        };

        for (int i = 0; i < attributes.length; i++) {
            MetadataAttribute attribute = attributes[i];
            String s = "at index [" + i + "]";
            assertNotNull(s, attribute);
            assertEquals(s, expectedNames[i], attribute.getName());
            assertEquals(s, 1, attribute.getNumDataElems());
            assertEquals(s, expectedValues[i], attribute.getData().getElemInt());
        }
    }
}
