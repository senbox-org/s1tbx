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

package org.esa.beam.csv.productio;

/**
 * Some constants.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class Constants {

    static final String[] LAT_NAMES = new String[]{"lat", "latitude", "northing"};
    static final String[] LON_NAMES = new String[]{"lon", "long", "longitude", "easting"};
    static final String[] TIME_NAMES = new String[]{"time", "date", "date_time", "dateTime"};
    static final String[] LOCATION_NAMES = new String[]{"name", "station", "label"};
    static final String TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
}
