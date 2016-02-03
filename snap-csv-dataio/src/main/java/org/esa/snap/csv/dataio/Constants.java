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

package org.esa.snap.csv.dataio;

/**
 * Some constants.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class Constants {

    public static final String[] LAT_NAMES = new String[]{"lat", "latitude", "northing"};
    public static final String[] LON_NAMES = new String[]{"lon", "long", "longitude", "easting"};
    public static final String[] TIME_NAMES = new String[]{"time", "date", "date_time", "dateTime"};
    public static final String[] LOCATION_NAMES = new String[]{"name", "station", "label"};
    public static final String[] CRS_IDENTIFIERS = new String[]{"crs"};
    public static final String TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DEFAULT_SEPARATOR = "\t";
    public static final String FORMAT_NAME = "CSV";
    public static final String DESCRIPTION = "CSV products";

    public static final String NAME_METADATA_ELEMENT_CSV_HEADER_PROPERTIES = "CSV Header Properties";

    public static final String PROPERTY_NAME_SEPARATOR = "separator";
    public static final String PROPERTY_NAME_TIME_COLUMN = "timeColumn";
    public static final String PROPERTY_NAME_TIME_PATTERN = "timePattern";
    public static final String PROPERTY_NAME_SCENE_RASTER_WIDTH = "sceneRasterWidth";

}
