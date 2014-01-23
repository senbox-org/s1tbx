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

public class ModisConstants {

    public static final String DEFAULT_FILE_EXTENSION = ".hdf";
    public static final String READER_DESCRIPTION = "MODIS HDF4 Data Products";
    public static final String FORMAT_NAME = "MODIS";

    public static final String NUM_OF_DAY_SCANS_KEY = "Number of Day mode scans";
    public static final String NUM_OF_NIGHT_SCANS_KEY = "Number of Night mode scans";
    public static final String CORE_META_KEY = "CoreMetadata\\.0";
    public static final String STRUCT_META_KEY = "StructMetadata\\.0";
    public static final String ARCHIVE_META_KEY = "ArchiveMetadata\\.0";

    public static final String GLOBAL_META_NAME = "GLOBAL_METADATA";

    public static final String GROUP_KEY = "GROUP";
    public static final String GROUP_END_KEY = "END_GROUP";
    public static final String DIMENSION_KEY = "Dimension";
    public static final String DIMENSION_MAP_KEY = "DimensionMap";
    public static final String GEO_FIELD_KEY = "GeoField";
    public static final String DATA_FIELD_KEY = "DataField";
    public static final String DIMENSION_NAME_KEY = "DimensionName";
    public static final String SIZE_KEY = "Size";
    public static final String OBJECT_KEY = "OBJECT";
    public static final String OBJECT_END_KEY = "END_OBJECT";
    public static final String GEO_DIMENSION_KEY = "GeoDimension";
    public static final String DATA_DIMENSION_KEY = "DataDimension";
    public static final String OFFSET_KEY = "Offset";
    public static final String INCREMENT_KEY = "Increment";
    public static final String GEO_FIELD_NAME_KEY = "GeoFieldName";
    public static final String DATA_TYPE_KEY = "DataType";
    public static final String DIMENSION_LIST_KEY = "DimList";
    public static final String DATA_FIELD_NAME_KEY = "DataFieldName";
    public static final String LOCAL_GRANULEID_KEY = "LOCALGRANULEID";
    public static final String SHORT_NAME_KEY = "SHORTNAME";

    public static final String BAND_NAMES_KEY = "band_names";
    public static final String VALID_RANGE_KEY = "valid_range";
    public static final String FILL_VALUE_KEY = "_FillValue";

    public static final String HDF_EOS_VERSION_KEY = "HDFEOSVersion";

    public static final String RANGE_BEGIN_DATE_KEY = "RANGEBEGINNINGDATE";
    public static final String RANGE_BEGIN_TIME_KEY = "RANGEBEGINNINGTIME";
    public static final String RANGE_END_DATE_KEY = "RANGEENDINGDATE";
    public static final String RANGE_END_TIME_KEY = "RANGEENDINGTIME";

    public static final float[] BAND_CENTER_WAVELENGTHS = new float[]{
            645.f, //  1
            858.5f, //  2
            469.f, //  3
            555.f, //  4
            1240.f, //  5
            1640.f, //  6
            2130.f, //  7
            412.5f, //  8
            443.f, //  9
            488.f, //  10
            531.f, //  11
            551.f, //  12
            667.f, //  13
            678.f, //  14
            748.f, //  15
            869.5f, //  16
            905.f, //  17
            936.f, //  18
            940.f, //  19
            3750.f, //  20
            3959.f, //  21
            3959.f, //  22  YES, they repeat this on the modis site: http://modis.gsfc.nasa.gov/about/specs.html
            4050.f, //  23
            4465.5f, //  24
            4515.5f, //  25
            1375.f, //  26
            6715.f, //  27
            7325.f, //  28
            8550.f, //  29
            9730.f, //  30
            11030.f, //  31
            12020.f, //  32
            13335.f, //  33
            13635.f, //  34
            13935.f, //  35
            14235.f     //  36
    };

    public static final float[] BAND_WIDTHS = new float[]{
            50.f, //  1
            35.f, //  2
            20.f, //  3
            20.f, //  4
            20.f, //  5
            24.f, //  6
            50.f, //  7
            15.f, //  8
            10.f, //  9
            10.f, //  10
            10.f, //  11
            10.f, //  12
            10.f, //  13
            10.f, //  14
            10.f, //  15
            15.f, //  16
            30.f, //  17
            10.f, //  18
            50.f, //  19
            180.f, //  20
            60.f, //  21
            60.f, //  22
            60.f, //  23
            65.f, //  24
            67.f, //  25
            30.f, //  26
            360.f, //  27
            300.f, //  28
            300.f, //  29
            300.f, //  30
            500.f, //  31
            500.f, //  32
            300.f, //  33
            300.f, //  34
            300.f, //  35
            300.f   //  36
    };

    public static final int[] SPECTRAL_BAND_INDEX = {
            7, // 1
            11, // 2
            2, // 3
            6, // 4
            16, // 5
            18, // 6
            9, // 7
            0, // 8
            1, // 9
            3, // 10
            4, // 11
            5, // 12
            8, // 13
            9, // 14
            10, // 15
            12, // 16
            13, // 17
            14, // 18
            15, // 19
            20, // 20
            21, // 21
            22, // 22
            23, // 23
            24, // 24
            25, // 25
            17, // 26
            26, // 27
            27, // 28
            28, // 29
            39, // 30
            30, // 31
            31, // 32
            32, // 33
            33, // 34
            34, // 35
            36  // 36
    };

    public static final char[] FIELD_SEPARATORS = {'|'};

    public static final String EXPONENTIAL_SCALE_NAME = "exp";
    public static final String LINEAR_SCALE_NAME = "lin";
    public static final String LINEAR_INVERTED_SCALE_NAME = "lin_inv";
    public static final String SLOPE_INTERCEPT_SCALE_NAME = "sli";
    public static final String POW_10_SCALE_NAME = "p10";

    // IMAPP specific stuff
    public static final String DAY_NIGHT_FLAG_KEY = "DAYNIGHTFLAG";
    public static final String DAY_NIGHT_FLAG_DAY_VALUE = "Day";
    public static final String BAND_NAME_KEY = "long_name";
    public static final String EOS_TYPE_GRID = "EOS_TYPE_GRID";
    public static final String EOS_TYPE_SWATH = "EOS_TYPE_SWATH";
    public static final String EOS_TYPE_POINT = "EOS_TYPE_POINT";

}
