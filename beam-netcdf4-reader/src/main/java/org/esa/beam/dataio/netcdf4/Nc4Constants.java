package org.esa.beam.dataio.netcdf4;

import java.io.File;

/**
 * Provides most of the constants used in this package.
 */
public interface Nc4Constants {

    String FORMAT_NAME = "NetCDF4_supported";
    String FORMAT_NAME_BEAM = "NcBEAM";
    String FORMAT_DESCRIPTION = "NetCDF supported Data Product";
    String FORMAT_DESCRIPTION_BEAM = "NetCDF supported BEAM Data Product";
    String FILE_EXTENSION_NC = ".nc";
    String FILE_EXTENSION_HDF = ".hdf";
    String[] FILE_EXTENSIONS = new String[]{FILE_EXTENSION_NC, FILE_EXTENSION_HDF};
    Class[] READER_INPUT_TYPES = new Class[]{
            String.class,
            File.class,
    };

    String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    String GLOBAL_ATTRIBUTES_NAME = "global_attributes";

    String SCALE_FACTOR_ATT_NAME = "scale_factor";
    String SLOPE_ATT_NAME = "slope";
    String ADD_OFFSET_ATT_NAME = "add_offset";
    String INTERCEPT_ATT_NAME = "intercept";
    String FILL_VALUE_ATT_NAME = "_FillValue";
    String MISSING_VALUE_ATT_NAME = "missing_value";
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

    // **********************
    // * Exception messages *
    // **********************
    String EM_INVALID_FLAG_CODING = "Invalid Flag Coding";
    String EM_INVALID_INDEX_CODING = "Invalid Index Coding";
    String EM_INVALID_COLOR_TABLE = "Invalid Color Table";
    String EM_INVALID_STX_ATTRIBUTES = "Invalid STX Attributes";
    String EM_INVALID_MASK_ATTRIBUTES = "Invalid Mask Attributes";
}
