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
package org.esa.beam.dataio.modis;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ModisConstantsTest extends TestCase {

    private static float[] expWavelength = {
            645.f, 858.5f, 469.f, 555.f, 1240.f, 1640.f, 2130.f, 412.5f, 443.f, 488.f,
            531.f, 551.f, 667.f, 678.f, 748.f, 869.5f, 905.f, 936.f, 940.f, 3750.f,
            3959.f, 3959.f, 4050.f, 4465.5f, 4515.5f, 1375.f, 6715.f, 7325.f, 8550.f,
            9730.f, 11030.f, 12020.f, 13335.f, 13635.f, 13935.f, 14235.f
    };
    private static float[] expBandwidth = {
            50.f,
            35.f,
            20.f,
            20.f,
            20.f,
            24.f,
            50.f,
            15.f,
            10.f,
            10.f,
            10.f,
            10.f,
            10.f,
            10.f,
            10.f,
            15.f,
            30.f,
            10.f,
            50.f,
            180.f,
            60.f,
            60.f,
            60.f,
            65.f,
            67.f,
            30.f,
            360.f,
            300.f,
            300.f,
            300.f,
            500.f,
            500.f,
            300.f,
            300.f,
            300.f,
            300.f
    };

    public ModisConstantsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ModisConstantsTest.class);
    }

    /**
     * Tests constants concerning general modis file constants
     */
    public void testFilePropertyConstants() {
        assertEquals(".hdf", ModisConstants.DEFAULT_FILE_EXTENSION);
        assertEquals("MODIS HDF4 Data Products", ModisConstants.READER_DESCRIPTION);
        assertEquals("MODIS", ModisConstants.FORMAT_NAME);
    }

    /**
     * Tests the motadata constant keys
     */
    public void testMetadataKeyConstants() {
        assertEquals("Number of Day mode scans", ModisConstants.NUM_OF_DAY_SCANS_KEY);
        assertEquals("Number of Night mode scans", ModisConstants.NUM_OF_NIGHT_SCANS_KEY);
        assertEquals("CoreMetadata.0", ModisConstants.CORE_META_KEY);
        assertEquals("StructMetadata.0", ModisConstants.STRUCT_META_KEY);
        assertEquals("GROUP", ModisConstants.GROUP_KEY);
        assertEquals("END_GROUP", ModisConstants.GROUP_END_KEY);
        assertEquals("Dimension", ModisConstants.DIMENSION_KEY);
        assertEquals("DimensionMap", ModisConstants.DIMENSION_MAP_KEY);
        assertEquals("GeoField", ModisConstants.GEO_FIELD_KEY);
        assertEquals("DataField", ModisConstants.DATA_FIELD_KEY);
        assertEquals("DimensionName", ModisConstants.DIMENSION_NAME_KEY);
        assertEquals("Size", ModisConstants.SIZE_KEY);
        assertEquals("OBJECT", ModisConstants.OBJECT_KEY);
        assertEquals("END_OBJECT", ModisConstants.OBJECT_END_KEY);
        assertEquals("GeoDimension", ModisConstants.GEO_DIMENSION_KEY);
        assertEquals("DataDimension", ModisConstants.DATA_DIMENSION_KEY);
        assertEquals("Offset", ModisConstants.OFFSET_KEY);
        assertEquals("Increment", ModisConstants.INCREMENT_KEY);
        assertEquals("GeoFieldName", ModisConstants.GEO_FIELD_NAME_KEY);
        assertEquals("DataType", ModisConstants.DATA_TYPE_KEY);
        assertEquals("DimList", ModisConstants.DIMENSION_LIST_KEY);
        assertEquals("DataFieldName", ModisConstants.DATA_FIELD_NAME_KEY);
        assertEquals("band_names", ModisConstants.BAND_NAMES_KEY);
        assertEquals("valid_range", ModisConstants.VALID_RANGE_KEY);
        assertEquals("_FillValue", ModisConstants.FILL_VALUE_KEY);

        assertEquals("GLOBAL_METADATA", ModisConstants.GLOBAL_META_NAME);
        assertEquals("LOCALGRANULEID", ModisConstants.LOCAL_GRANULEID_KEY);
        assertEquals("SHORTNAME", ModisConstants.SHORT_NAME_KEY);

        assertEquals("HDFEOSVersion", ModisConstants.HDF_EOS_VERSION_KEY);

        assertEquals("RANGEBEGINNINGDATE", ModisConstants.RANGE_BEGIN_DATE_KEY);
        assertEquals("RANGEBEGINNINGTIME", ModisConstants.RANGE_BEGIN_TIME_KEY);
        assertEquals("RANGEENDINGDATE", ModisConstants.RANGE_END_DATE_KEY);
        assertEquals("RANGEENDINGTIME", ModisConstants.RANGE_END_TIME_KEY);
    }

    /**
     * Tests the wavelength and bandwidth constants for correctness
     */
    public void testWavelengthsAndBandwidths() {
        assertEquals(expBandwidth.length, ModisConstants.BAND_WIDTHS.length);
        assertEquals(expWavelength.length, ModisConstants.BAND_CENTER_WAVELENGTHS.length);
        assertEquals(ModisConstants.BAND_WIDTHS.length, ModisConstants.BAND_CENTER_WAVELENGTHS.length);

        for (int n = 0; n < ModisConstants.BAND_CENTER_WAVELENGTHS.length; n++) {
            assertEquals(expBandwidth[n], ModisConstants.BAND_WIDTHS[n], 1e-6);
            assertEquals(expWavelength[n], ModisConstants.BAND_CENTER_WAVELENGTHS[n], 1e-6);
        }
    }

    public void testImappConstants() {
        assertEquals("DAYNIGHTFLAG", ModisConstants.DAY_NIGHT_FLAG_KEY);
        assertEquals("Day", ModisConstants.DAY_NIGHT_FLAG_DAY_VALUE);
        assertEquals("long_name", ModisConstants.BAND_NAME_KEY);
    }
}
