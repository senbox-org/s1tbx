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

package org.esa.snap.dataio.netcdf.util;

import java.io.File;

/**
 * Provides most of the constants used in this package.
 */
public interface Constants {

    String FORMAT_NAME = "NetCDF";
    String FORMAT_DESCRIPTION = "NetCDF/CF Data Product";
    String FILE_EXTENSION_NC = ".nc";
    String FILE_EXTENSION_NC_GZ = ".nc.gz";
    String FILE_EXTENSION_HDF = ".hdf";
    String FILE_EXTENSION_HDF_GZ = ".hdf.gz";
    String FILE_EXTENSION_HDF_BZ2 = ".hdf.bz2";
    String[] FILE_EXTENSIONS = new String[]{
            FILE_EXTENSION_NC, FILE_EXTENSION_NC_GZ,
            FILE_EXTENSION_HDF, FILE_EXTENSION_HDF_GZ,
            FILE_EXTENSION_HDF_BZ2,
    };
    Class[] READER_INPUT_TYPES = new Class[]{
            String.class,
            File.class,
    };

    String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    String SCALE_FACTOR_ATT_NAME = "scale_factor";
    String SLOPE_ATT_NAME = "slope";
    String ADD_OFFSET_ATT_NAME = "add_offset";
    String INTERCEPT_ATT_NAME = "intercept";
    String FILL_VALUE_ATT_NAME = "_FillValue";
    String MISSING_VALUE_ATT_NAME = "missing_value";
    String ORIG_NAME_ATT_NAME = "orig_name";
    String VALID_MIN_ATT_NAME = "valid_min";
    String VALID_MAX_ATT_NAME = "valid_max";
    String STEP_ATT_NAME = "step";
    String START_DATE_ATT_NAME = "start_date";
    String START_TIME_ATT_NAME = "start_time";
    String STOP_DATE_ATT_NAME = "stop_date";
    String STOP_TIME_ATT_NAME = "stop_time";

    String LON_VAR_NAME = "lon";
    String LAT_VAR_NAME = "lat";
    String LONGITUDE_VAR_NAME = "longitude";
    String LATITUDE_VAR_NAME = "latitude";

    // context properties
    String Y_FLIPPED_PROPERTY_NAME = "yFlipped";
    String CONVERT_LOGSCALED_BANDS_PROPERTY = "convertLogScaledBands";
    String PRODUCT_FILENAME_PROPERTY = "productName";

}
