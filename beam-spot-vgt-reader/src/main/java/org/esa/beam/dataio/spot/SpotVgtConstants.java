/*
 * Copyright (C) 2010  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.spot;

public class SpotVgtConstants {

    public static final String PHYS_VOL_FILENAME = "PHYS_VOL.TXT";

    public static final String FILE_EXTENSION = "TXT";
    public static final String DEFAULT_DATA_DIR_NAME = "0001";
    public static final String READER_DESCRIPTION = "SPOT VGT Data Products";
    public static final String FORMAT_NAME = "SPOT-VGT";

    // Meta data attributes
    public static final String ATTR_NAME_IMAGE_NUMBER = "Image Number";
    public static final String ATTR_NAME_CHRIS_MODE = "CHRIS Mode";
    public static final String ATTR_NAME_NUMBER_OF_SAMPLES = "Number of Samples";
    public static final String ATTR_NAME_NUMBER_OF_GROUND_LINES = "Number of Ground Lines";
    public static final String ATTR_NAME_IMAGE_FLIPPED_ALONG_TRACK = "Image Flipped Along-Track";


    // Scientific data set names
    static final String SDS_NAME_RCI_IMAGE = "RCI Image";
    static final String SDS_NAME_MASK = "Saturation/Reset Mask";

    static final String VS_NAME_GAIN_INFO = "Gain Information";
    static final String VS_NAME_GAIN_SETTING = "Gain Setting";
    static final String VS_NAME_GAIN_VALUE = "Gain Value";
    static final String VS_NAME_MODE_INFO = "Mode Information";
    static final String[] VS_NAME_MODE_FIELDS = {"WlLow","WlHigh","WlMid","BWidth","Gain","Low Row","High Row"};

}
