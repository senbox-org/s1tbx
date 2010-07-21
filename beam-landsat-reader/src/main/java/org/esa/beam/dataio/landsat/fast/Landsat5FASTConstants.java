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

package org.esa.beam.dataio.landsat.fast;

import org.esa.beam.dataio.landsat.LandsatConstants;


/**
 * The class <code>Landsat5FASTConstants</code> is used to store constant data which are specific for the Landsat 5 satellite
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public class Landsat5FASTConstants extends LandsatConstants {

    public final static String L5_HEADER_FILE_NAME = "header";
    public static final String L5_PRODUCT_NAME = "LANDSAT 5 TM SCENE";
    public static final int FASTB_HEADER_DECODE_VALUES = 93;
    public static final int NUMBER_OF_L5_BANDS = 7;

    /**
     * Values of FAST REV.B Format Specification
     * Dataset structure:
     * <p/>
     * Keyword: 					String Value
     * Start byte of data value: 	Int Value
     * Length of the data field:	Int Value
     */
    static final int PRODUCT_ID_OFFSET_FASTB = 10;
    static final int PRODUCT_ID_SIZE_FASTB = 11;

    static final int PATH_ROW_FRACTION_OFFSET_FASTB = 27;
    static final int PATH_ROW_FRACTION_SIZE_FASTB = 9;

    static final int DATE_OF_AQUISITION_OFFSET_FASTB = 55;
    static final int DATE_OF_AQUISITION_SIZE_FASTB = 8;

    public static final int SATELLITE_OFFSET_FASTB = 75;
    public static final int SATELLITE_SIZE_FASTB = 2;

    static final int INSTRUMENT_OFFSET_FASTB = 90;
    public static final int INSTRUMENT_SIZE_FASTB = 2;

    static final int INSTRUMENT_MODE_OFFSET_FASTB = 92;
    static final int INSTRUMENT_MODE_SIZE_FASTB = 2;

    static final int TYPE_OF_PRODUCT_OFFSET_FASTB = 109;
    static final int TYPE_OF_PRODUCT_SIZE_FASTB = 14;

    static final int SIZE_OF_PRODUCT_OFFSET_FASTB = 138;
    static final int SIZE_OF_PRODUCT_SIZE_FASTB = 15;

    static final int TYPE_OF_PROCESSING_OFFSET_FASTB = 256;
    static final int TYPE_OF_PROCESSING_SIZE_FASTB = 10;

    static final int RESAMPLING_OFFSET_FASTB = 279;
    static final int RESAMPLING_SIZE_FASTB = 2;

    static final int TAPE_SPANNING_FLAG_OFFSET_FASTB = 439;
    static final int TAPE_SPANNING_FLAG_SIZE_FASTB = 3;

    static final int PIXEL_SIZE_OFFSET_FASTB = 1064;
    static final int PIXEL_SIZE_SIZE_FASTB = 5;

    static final int PIXELS_PER_LINE_OFFSET_FASTB = 1086;
    static final int PIXELS_PER_LINE_SIZE_FASTB = 5;

    static final int LINES_PER_IMAGE_OFFSET_FASTB = 1108;
    static final int LINES_PER_IMAGE_SIZE_FASTB = 5;

    static final int START_LINE_OFFSET_FASTB = 456;
    static final int START_LINE_SIZE_FASTB = 5;

    static final int LINES_PER_VOLUMES_OFFSET_FASTB = 476;
    static final int LINES_PER_VOLUMES_SIZE_FASTB = 5;

    static final int RECORD_LENGTH_OFFSET_FASTB = 1406;
    static final int RECORD_LENGTH_SIZE_FASTB = 5;

    static final int BLOCKING_FACTOR_OFFSET_FASTB = 1386;
    static final int BLOCKING_FACTOR_SIZE_FASTB = 4;

    public static final String FORMAT_VERSION_FASTB = " REV";
    static final int FORMAT_VERSION_OFFSET_FASTB = 1536;
    static final int FORMAT_VERSION_SIZE_FASTB = 1;

    static final int BANDS_PRESENT_OFFSET_FASTB = 1361;
    static final int BANDS_PRESENT_SIZE_FASTB = 7;

    /**
     * geometric data constants
     */
    static final int LOOK_ANGLE_OFFSET_FASTB = 495;
    static final int LOOK_ANGLE_SIZE_FASTB = 6;

    static final int SUN_ELEVATION_OFFSET_FASTB = 1427;
    static final int SUN_ELEVATION_SIZE_FASTB = 2;

    static final int SUN_AZIMUTH_OFFSET_FASTB = 1443;
    static final int SUN_AZIMUTH_SIZE_FASTB = 3;

    static final int HORIZONTAL_OFFSET_OFFSET_FASTB = 1528;
    static final int HORIZONTAL_OFFSET_SIZE_FASTB = 4;

    static final int PROJECTION_OFFSET_FASTB = 514;
    static final int PROJECTION_SIZE_FASTB = 4;

    static final int ELLIPSOID_OFFSET_FASTB = 973;
    static final int ELLIPSOID_SIZE_FASTB = 20;

    static final int SEMI_MAJOR_AXIS_OFFSET_FASTB = 1011;
    static final int SEMI_MAJOR_AXIS_SIZE_FASTB = 11;

    static final int SEMI_MINOR_AXIS_OFFSET_FASTB = 1040;
    static final int SEMI_MINOR_AXIS_SIZE_FASTB = 11;

    static final int PROJECTION_NUMBER_OFFSET_FASTB = 538;
    static final int PROJECTION_NUMBER_SIZE_FASTB = 6;

    static final int MAP_ZONE_OFFSET_FASTB = 560;
    static final int MAP_ZONE_SIZE_FASTB = 6;

    static final int PROJECTION_PARAMETERS_OFFSET_FASTB = 595;
    static final int PROJECTION_PARAMETERS_SIZE_FASTB = 360;

    public static final String UPPER_LEFTER_CORNER_FASTB = " UL ";
    static final int UPPER_LEFT_CORNER_LONGITUDE_OFFSET_FASTB = 1117;
    static final int UPPER_LEFT_CORNER_LATITUDE_OFFSET_FASTB = 1131;
    static final int UPPER_LEFT_EASTING_OFFSET_FASTB = 1144;
    static final int UPPER_LEFT_NORTHING_OFFSET_FASTB = 1158;

    static final int UPPER_RIGHT_CORNER_LONGITUDE_OFFSET_FASTB = 1175;
    static final int UPPER_RIGHT_CORNER_LATITUDE_OFFSET_FASTB = 1189;
    static final int UPPER_RIGHT_EASTING_OFFSET_FASTB = 1202;
    static final int UPPER_RIGHT_NORTHING_OFFSET_FASTB = 1216;

    static final int LOWER_RIGHT_CORNER_LONGITUDE_OFFSET_FASTB = 1233;
    static final int LOWER_RIGHT_CORNER_LATITUDE_OFFSET_FASTB = 1247;
    static final int LOWER_RIGHT_EASTING_OFFSET_FASTB = 1260;
    static final int LOWER_RIGHT_NORTHING_OFFSET_FASTB = 1274;

    static final int LOWER_LEFT_CORNER_LONGITUDE_OFFSET_FASTB = 1291;
    static final int LOWER_LEFT_CORNER_LATITUDE_OFFSET_FASTB = 1305;
    static final int LOWER_LEFT_EASTING_OFFSET_FASTB = 1318;
    static final int LOWER_LEFT_NORTHING_OFFSET_FASTB = 1332;

    static final int CENTER_LONGITUDE_OFFSET_FASTB = 1454;
    static final int CENTER_LATITUDE_OFFSET_FASTB = 1468;
    static final int CENTER_EASTING_OFFSET_FASTB = 1481;
    static final int CENTER_NORTHING_OFFSET_FASTB = 1495;
    static final int CENTER_PIXEL_NUMBER_OFFSET_FASTB = 1508;
    static final int CENTER_LINE_NUMBER_OFFSET_FASTB = 1514;

    public static final int HEMISPHERE_VALUE_SIZE = 1;

    static final int CENTER_NUMBERS_SIZE_FASTB = 6;

    static final int LONGITUDE_SIZE_FASTB = 13;
    static final int LATITUDE_SIZE_FASTB = 12;
    static final int EASTING_NORTHING_SIZE_FASTB = 13;

    /**
     * Radiometric data constants
     */
    static final int RADIANCE_VALUE_DATA_SIZE = 8;
    static final int RADIOMETRIC_DATA_FASTB_OFFSET = 301;
    public static final int RADIOMETRIC_DATA_FASTB_SIZE = 118;


    /**
     * Specific variables
     */

    public static final int LENGTH_OF_FASTB_LOCATION_STRING = 9;
    static final String YYYY_MMDD = "yyyyMMdd";

}
