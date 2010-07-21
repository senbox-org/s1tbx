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
    public static final String BAND_INFORMATION_NAME = "Band Information";
    // Meta data attributes
    public static final String ATTR_NAME_SENSOR_TYPE = "Sensor Type";
    public static final String ATTR_NAME_DATA_RIGHTS = "Data Rights";
    public static final String ATTR_NAME_TARGET_NAME = "Target Name";
    public static final String ATTR_NAME_IMAGE_DATE = "Image Date";
    public static final String ATTR_NAME_IMAGE_NUMBER = "Image Number";
    public static final String ATTR_NAME_IMAGE_TAG = "Image Tag";
    public static final String ATTR_NAME_TARGET_LON = "Target Longitude";
    public static final String ATTR_NAME_TARGET_LAT = "Target Latitude";
    public static final String ATTR_NAME_TARGET_ALT = "Target Altitude";
    public static final String ATTR_NAME_FLY_BY_ZENITH_ANGLE = "Fly-by Zenith Angle";
    public static final String ATTR_NAME_MINIMUM_ZENITH_ANGLE = "Minimum Zenith Angle";
    public static final String ATTR_NAME_SOLAR_ZENITH_ANGLE = "Solar Zenith Angle";
    public static final String ATTR_NAME_SOLAR_AZIMUTH_ANGLE = "Solar Azimuth Angle";
    public static final String ATTR_NAME_FLY_BY_TIME = "Fly-by Time";
    public static final String ATTR_NAME_IMAGE_CENTRE_TIME = "Calculated Image Centre Time";
    public static final String ATTR_NAME_OBSERVATION_ZENITH_ANGLE = "Observation Zenith Angle";
    public static final String ATTR_NAME_OBSERVATION_AZIMUTH_ANGLE = "Observation Azimuth Angle";
    public static final String ATTR_NAME_CHRIS_MODE = "CHRIS Mode";
    public static final String ATTR_NAME_NUMBER_OF_SAMPLES = "Number of Samples";
    public static final String ATTR_NAME_NUMBER_OF_GROUND_LINES = "Number of Ground Lines";
    public static final String ATTR_NAME_NUMBER_OF_BANDS = "Number of Bands";
    public static final String ATTR_NAME_PLATFORM_ALTITUDE = "Platform Altitude";
    public static final String ATTR_NAME_RESPONSE_FILE_CREATION_TIME = "Response File Creation Time";
    public static final String ATTR_NAME_DARK_FILE_CREATION_TIME = "Dark File Creation Time";
    public static final String ATTR_NAME_CALIBRATION_DATA_UNITS = "Calibration Data Units";
    public static final String ATTR_NAME_CHRIS_TEMPERATURE = "CHRIS Temperature";
    public static final String ATTR_NAME_KEY_TO_MASK = "Key to Mask";
    public static final String ATTR_NAME_IMAGE_FLIPPED_ALONG_TRACK = "Image Flipped Along-Track";

    public static final String ATTR_NAME_NOISE_REDUCTION = "Noise Reduction";

    // Scientific data set names
    static final String SDS_NAME_RCI_IMAGE = "RCI Image";
    static final String SDS_NAME_MASK = "Saturation/Reset Mask";

    static final String VS_NAME_GAIN_INFO = "Gain Information";
    static final String VS_NAME_GAIN_SETTING = "Gain Setting";
    static final String VS_NAME_GAIN_VALUE = "Gain Value";
    static final String VS_NAME_MODE_INFO = "Mode Information";
    static final String[] VS_NAME_MODE_FIELDS = {"WlLow","WlHigh","WlMid","BWidth","Gain","Low Row","High Row"};

}
