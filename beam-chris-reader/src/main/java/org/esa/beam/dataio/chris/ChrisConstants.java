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
package org.esa.beam.dataio.chris;

public class ChrisConstants {

    public static final String DEFAULT_FILE_EXTENSION = "hdf";
    public static final String READER_DESCRIPTION = "CHRIS/Proba HDF4 Data Products";
    public static final String FORMAT_NAME = "CHRIS/Proba";

    public static final String MPH_NAME = "MPH";
    public static final String SPH_NAME = "SPH";
    public static final String BAND_INFORMATION_NAME = "Band_Information";
    // Meta data attributes
    public static final String ATTR_NAME_SENSOR_TYPE = "Sensor_Type";
    public static final String ATTR_NAME_DATA_RIGHTS = "Data_Rights";
    public static final String ATTR_NAME_TARGET_NAME = "Target_Name";
    public static final String ATTR_NAME_IMAGE_DATE = "Image_Date";
    public static final String ATTR_NAME_IMAGE_NUMBER = "Image_Number";
    public static final String ATTR_NAME_IMAGE_TAG = "Image_Tag";
    public static final String ATTR_NAME_TARGET_LON = "Target_Longitude";
    public static final String ATTR_NAME_TARGET_LAT = "Target_Latitude";
    public static final String ATTR_NAME_TARGET_ALT = "Target_Altitude";
    public static final String ATTR_NAME_FLY_BY_ZENITH_ANGLE = "Fly-by_Zenith_Angle";
    public static final String ATTR_NAME_MINIMUM_ZENITH_ANGLE = "Minimum_Zenith_Angle";
    public static final String ATTR_NAME_SOLAR_ZENITH_ANGLE = "Solar_Zenith_Angle";
    public static final String ATTR_NAME_SOLAR_AZIMUTH_ANGLE = "Solar_Azimuth_Angle";
    public static final String ATTR_NAME_FLY_BY_TIME = "Fly-by_Time";
    public static final String ATTR_NAME_IMAGE_CENTRE_TIME = "Calculated_Image_Centre_Time";
    public static final String ATTR_NAME_OBSERVATION_ZENITH_ANGLE = "Observation_Zenith_Angle";
    public static final String ATTR_NAME_OBSERVATION_AZIMUTH_ANGLE = "Observation_Azimuth_Angle";
    public static final String ATTR_NAME_CHRIS_MODE = "CHRIS_Mode";
    public static final String ATTR_NAME_NUMBER_OF_SAMPLES = "Number_of_Samples";
    public static final String ATTR_NAME_NUMBER_OF_GROUND_LINES = "Number_of_Ground_Lines";
    public static final String ATTR_NAME_NUMBER_OF_BANDS = "Number_of_Bands";
    public static final String ATTR_NAME_PLATFORM_ALTITUDE = "Platform_Altitude";
    public static final String ATTR_NAME_RESPONSE_FILE_CREATION_TIME = "Response_File_Creation_Time";
    public static final String ATTR_NAME_DARK_FILE_CREATION_TIME = "Dark_File_Creation_Time";
    public static final String ATTR_NAME_CALIBRATION_DATA_UNITS = "Calibration_Data_Units";
    public static final String ATTR_NAME_CHRIS_TEMPERATURE = "CHRIS_Temperature";
    public static final String ATTR_NAME_KEY_TO_MASK = "Key_to_Mask";
    public static final String ATTR_NAME_IMAGE_FLIPPED_ALONG_TRACK = "Image_Flipped_Along-Track";

    public static final String ATTR_NAME_NOISE_REDUCTION = "Noise Reduction";

    // Scientific data set names
    static final String SDS_NAME_RCI_IMAGE = "RCI_Image";
    static final String SDS_NAME_MASK = "Saturation_Reset_Mask";

    static final String VS_NAME_GAIN_INFO = "Gain_Information";
    static final String VS_NAME_GAIN_SETTING = "Gain_Setting";
    static final String VS_NAME_GAIN_VALUE = "Gain_Value";
    static final String VS_NAME_MODE_INFO = "Mode_Information";
    static final String[] VS_NAME_MODE_FIELDS = {"WlLow","WlHigh","WlMid","BWidth","Gain","Low_Row","High_Row"};

}
