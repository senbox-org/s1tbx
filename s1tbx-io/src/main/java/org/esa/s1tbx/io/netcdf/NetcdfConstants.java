/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.netcdf;

import org.esa.snap.dataio.netcdf.util.Constants;

/**
 * Provides most of the constants used in this package.
 */
public interface NetcdfConstants extends Constants {

    final static String[] NETCDF_FORMAT_NAMES = {"NetCDF"};
    final static String[] NETCDF_FORMAT_FILE_EXTENSIONS = {"nc", "nc3"};
    final static String NETCDF_PLUGIN_DESCRIPTION = "NetCDF Products";

    final static String GLOBAL_ATTRIBUTES_NAME = "Global_Attributes";

    final static String DESCRIPTION = "description";
    final static String UNIT = "unit";

    final static String UTC_TYPE = "utc:";

    // CF convention lon
    // COARDS convention longitude
    // Enviview longs first_line_tie_points.longs
    final static String[] LON_VAR_NAMES = {"lon", "longitude", "longs", "first_line_tie_points.longs"};
    final static String[] LAT_VAR_NAMES = {"lat", "latitude", "lats", "first_line_tie_points.lats"};
}
