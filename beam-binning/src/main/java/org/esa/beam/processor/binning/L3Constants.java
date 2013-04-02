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
package org.esa.beam.processor.binning;

import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;

@Deprecated
/**
 * Provides an interface defining all constants used with the L3 processor.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3Constants implements ProcessorConstants {

    // The required request type
    public static final String REQUEST_TYPE = "BINNING";

    // name of the parameter composite_type
    public static final String RESAMPLING_TYPE_PARAM_NAME = "resampling_type";
    public static final String RESAMPLING_TYPE_PARAM_LABEL = "Resampling";
    public static final String RESAMPLING_TYPE_PARAM_DESC = "The method of the products composition";
    // value for binning composite type
    public static final String RESAMPLING_TYPE_VALUE_BINNING  = "binning";
    // value for clipping resampling composite type
    public static final String RESAMPLING_TYPE_VALUE_FLUX_CONSERVING = "flux-conserving";

    // name of the parameter composite_type
    public static final String TAILORING_PARAM_NAME = "tailoring";
    public static final String TAILORING_PARAM_LABEL = "Tailor output to geographic boundary of input";
    public static final String TAILORING_PARAM_DESC = "Determines the product geographic boundary";
    public static final Boolean TAILORING_DEFAULT_VALUE = Boolean.TRUE;

    // name of the parameter process_type
    public static final String PROCESS_TYPE_PARAM_NAME = "process_type";
    // value for proceess type init
    public static final String PROCESS_TYPE_INIT = "init";
    // value for proceess type update
    public static final String PROCESS_TYPE_UPDATE = "update";
    // value for proceess type finalize
    public static final String PROCESS_TYPE_FINALIZE = "finalize";
    // the value set of the patrameter process_type
    public static final String[] PROCESS_TYPE_VALUE_SET = new String[]{PROCESS_TYPE_INIT,
                                                                       PROCESS_TYPE_UPDATE,
                                                                       PROCESS_TYPE_FINALIZE};
    // default value for the parameter process_type
    public static final String PROCESS_TYPE_DEFAULT_VALUE = PROCESS_TYPE_INIT;

    // user database directory
    public static final String USER_DB_DIR = "db_dir";
    // user input directory
    public static final String USER_INPUT_DIR = "input_dir";

    // the default value for the output product
    public static final String DEFAULT_FILE_NAME = "l3_out.dim";

    public static final String DEFAULT_LOGFILE_PREFIX = "l3";

    // name of the parameter database_dir
    public static final String DATABASE_PARAM_NAME = "database";
    // label of the parameter database_dir
    public static final String DATABASE_LABEL = "Bin-Database";
    // tooltip text of the parameter database
    public static final String DATABASE_DESCRIPTION = "Bin-Database directory path";
    // default path for the bin database
    public static final String DEFAULT_DATABASE_NAME = "l3_database.bindb";

    // name of the parameter grid_cell_size
    public static final String GRID_CELL_SIZE_PARAM_NAME = "grid_cell_size";
    // label of the parameter grid_cell_size
    public static final String GRID_CELL_SIZE_LABEL = "Grid cell size";
    // tooltip text of the parameter grid_cell_size
    public static final String GRID_CELL_SIZE_DESCRIPTION = "Grid cell size in km";
    // physical unit of the parameter grid_cell_size
    public static final String GRID_CELL_SIZE_UNIT = "km (in sinusoidal grid)";
    // default value for parameter grid_cell_size
    public static final Float GRID_CELL_SIZE_DEFAULT = 9.28f;
    // the allowed min/max value for the cell size
    public static final Float GRID_CELL_SIZE_MIN_VALUE = 0.0001f;
    public static final Float GRID_CELL_SIZE_MAX_VALUE = BinDatabaseConstants.PI_EARTH_RADIUS;

    // name of the parameter cells_per_degree
    public static final String CELLS_PER_DEGREE_PARAM_NAME = "cells_per_degree";
    // label of the parameter cells_per_degree
    public static final String CELLS_PER_DEGREE_LABEL = "Cells per degree";
    // tooltip text of the parameter cells_per_degree
    public static final String CELLS_PER_DEGREE_DESCRIPTION = "Number of grid cells per degree";
    // physical unit of the parameter cells_per_degree
    public static final String CELLS_PER_DEGREE_UNIT = "1 / deg";
    // default value for parameter cells_per_degree
    // 30 bins per degree correspond approximately to 3.7km resolution per bin
    public static final Integer CELLS_PER_DEGREE_DEFAULT = 30;
    // the allowed min/max value for the cell size
    public static final Integer CELLS_PER_DEGREE_MIN_VALUE = 1;
    public static final Integer CELLS_PER_DEGREE_MAX_VALUE = 1000000;



    // name of the parameter band_names
    public static final String BAND_NAME_PARAMETER_NAME = "band_name";
    // label string of the parameter band name
    public static final String BAND_NAME_LABEL = "Band name";
    // Description of the parameter band name
    public static final String BAND_NAME_DESCRIPTION = "Name of the binned geophysical parameter";

    public static final String BAND_NUM_PARAMETER_NAME = "band_num";


    public static final String ALGORITHM_VALUE_MAXIMUM_LIKELIHOOD = "Maximum Likelihood";
    public static final String ALGORITHM_VALUE_ARITHMETIC_MEAN = "Arithmetic Mean";
    public static final String ALGORITHM_VALUE_MINIMUM_MAXIMUM = "Minimum/Maximum";

    // name of the parameter binning_algorithm
    public static final String ALGORITHM_PARAMETER_NAME = "binning_algorithm";
    // the parameter label
    public static final String ALGORITHM_LABEL = "Aggregation algorithm";
    // the parameter description
    public static final String ALGORITHM_DESCRIPTION = "Sample value aggregation algorithm name";
    // the value set of the parameter binning_algorithm
    public static final String[] ALGORITHM_VALUE_SET = {ALGORITHM_VALUE_ARITHMETIC_MEAN,
                                                        ALGORITHM_VALUE_MAXIMUM_LIKELIHOOD,
                                                        ALGORITHM_VALUE_MINIMUM_MAXIMUM};
    // default value for the parameter binning_algrithm
    public static final String ALGORITHM_DEFAULT_VALUE = ALGORITHM_VALUE_ARITHMETIC_MEAN;

    // name of the parameter weight_coefficient
    public static final String WEIGHT_COEFFICIENT_PARAMETER_NAME = "weight_coefficient";
    // the parameter label
    public static final String WEIGHT_COEFFICIENT_LABEL = "Weight coefficient";
    // The parameter description
    public static final String WEIGHT_COEFFICIENT_DESCRIPTION = "Weight coefficient used for averaging";
    // default value for the parameter weight_coefficient
    public static final Float WEIGHT_COEFFICIENT_DEFAULT_VALUE = 0.5f;

    // name of the parameter bitmask
    public static final String BITMASK_PARAMETER_NAME = "bitmask";
    // label of the parameter bitmask
    public static final String BITMASK_LABEL = "Bitmask expression";
    // the parameter description
    public static final String BITMASK_DESCRIPTION = "Bitmask expression determining valid input pixels";

    // name of the parameter "delete_db"
    public static final String DELETE_DB_PARAMETER_NAME = "delete_db";
    // label of the parameter "delete_db"
    public static final String DELETE_DB_LABEL = "Delete bin-database";
    // the description of the arameter "delete_db"
    public static final String DELETE_DB_DESCRIPTION = "Whether or not to delete bin-database";
    // defaiult value of the parameter "delete_db"
    public static final Boolean DELETE_DB_DEFAULT_VALUE = false;

    // the physical unit of the parameter
    public static final String LAT_LON_PHYS_UNIT = "degree";

    // name of the parameter lat_min
    public static final String LAT_MIN_PARAMETER_NAME = "lat_min";
    // default value of the parameter lat_min
    public static final Float LAT_MIN_DEFAULT_VALUE = -90.f;
    // the parameter label
    public static final String LAT_MIN_LABEL = "Min. latitude";
    // the parameter description
    public static final String LAT_MIN_DESCRIPTION = "Minimum latitude considered for binning";

    // name of the parameter lat_max
    public static final String LAT_MAX_PARAMETER_NAME = "lat_max";
    // default value of the parameter lat_max
    public static final Float LAT_MAX_DEFAULT_VALUE = 90.f;
    // the parameter label
    public static final String LAT_MAX_LABEL = "Max. latitude";
    // the parameter description
    public static final String LAT_MAX_DESCRIPTION = "Maximum latitude considered for binning";

    // name of the parameter lon_min
    public static final String LON_MIN_PARAMETER_NAME = "lon_min";
    // the parameter label
    public static final String LON_MIN_LABEL = "Min. longitude";
    // the parameter description
    public static final String LON_MIN_DESCRIPTION = "Minimum longitude considered for binning";
    // default value of the parameter lon_min
    public static final Float LON_MIN_DEFAULT_VALUE = -180.f;

    // name of the parameter lon_max
    public static final String LON_MAX_PARAMETER_NAME = "lon_max";
    // the parameter label
    public static final String LON_MAX_LABEL = "Max. longitude";
    // the parameter description
    public static final String LON_MAX_DESCRIPTION = "Maximum longitude considered for binning";
    // default value of the parameter lon_max
    public static final Float LON_MAX_DEFAULT_VALUE = 180.f;

    public static final Float LAT_MINIMUM_VALUE = -90.f;
    public static final Float LAT_MAXIMUM_VALUE = 90.f;
    public static final Float LON_MINIMUM_VALUE = -180.f;
    public static final Float LON_MAXIMUM_VALUE = 180.f;

    // some detailed messages
    public static final String MSG_MISSING_BINDB = "Please supply a valid bin-database.";
    public static final String MSG_MISSING_CELL_SIZE = "Please supply the desired grid cell size.";
    public static final String MSG_MISSING_ALGORITHM = "Please select an aggregation algorithm.";
    public static final String MSG_MISSING_BAND = "Please select the geophysical band to be processed.";
    public static final String MSG_MISSING_BITMASK = "Please supply a bitmask expression or an empty string, if no bitmask shall be taken into account.";
    public static final String MSG_MISSING_DELETE_BINDB = "Please select whether to keep or remove the bin-database.";

    public static final String LOGGER_NAME = "beam.processor.binning";
    public static final String DEFAULT_LOG_PREFIX_INIT = "l3_initial";
    public static final String DEFAULT_LOG_PREFIX_UPDATE = "l3_update";
    public static final String DEFAULT_LOG_PREFIX_FINAL = "l3_final";

    public static final String LOG_MSG_ERROR_WRONG_ACCUM = "Incorrect accumulation class: ";
    public static final String LOG_MSG_ERROR_CLOSE_BINDB = "Error closing bin database: ";
    public static final String LOG_MSG_LOAD_TEMP_DB = "Loading temporal database";
    public static final String LOG_MSG_TEMP_DB_NOT_FOUND_1 = "The temporal database in the directory'";
    public static final String LOG_MSG_TEMP_DB_NOT_FOUND_2 = "' could not be found!";
    public static final String LOG_MSG_EMPTY_DB = "No input products have been processed, the bin database is empty.\n" +
                                                  "Possible reasons for this are:\n" +
                                                  "  1) All input products are located outside in the geographical area\n" +
                                                  "  2) All input products are incompatible with the bin database\n";
    public static final String LOG_MSG_DELETE_TEMP_DB = "Deleting temporal database ";
    public static final String LOG_MSG_DELETE_FINAL_DB = "Deleting final database ";
    public static final String LOG_MSG_CREATE_FINAL_DB = "Creating final database ";
    public static final String LOG_MSG_INTERPRETE_BIN_CONTENT = "Reading bin contents ";
    public static final String LOG_MSG_CREATE_OUTPUT = "Creating output product ";
    public static final String LOG_MSG_OUTPUT_DIM_1 = "... product dimension: width : ";
    public static final String LOG_MSG_OUTPUT_DIM_2 = " height: ";
    public static final String LOG_MSG_CALC_PROJ_PARAM = "Calculating projection parameter ";
    public static final String LOG_MSG_PROJ_BORDER = "... projection raster borders: ";
    public static final String LOG_MSG_LAT_MIN = "...... latitude minimum: ";
    public static final String LOG_MSG_LAT_MAX = " latitude maximum: ";
    public static final String LOG_MSG_LON_MIN = "...... longitude minimum: ";
    public static final String LOG_MSG_LON_MAX = " longitude maximum: ";
    public static final String LOG_MSG_APPLY_PROJ = "Applying map projection ";
    public static final String LOG_MSG_CREATE_BIN_DB = "Creating bin database ";
    public static final String LOG_MSG_INVALID_REQUEST_TYPE = "Invalid request: must be of process_type 'init', 'update' or 'finalize'";
    public static final String LOG_MSG_HEADER = "Logfile generated by BEAM L3 Processor, version ";
    public static final String LOG_MSG_ERROR_OPEN_BINDB = "Could not open the bin database at: ";
    public static final String LOG_MSG_CLOSE_BINDB = "Closing bin database ";
    public static final String LOG_MSG_INPUT_NOT_FOUND_1 = "The input product number ";
    public static final String LOG_MSG_INPUT_NOT_FOUND_2 = " could not be read from the request file!";
    public static final String LOG_MSG_INPUT_NOT_EXIST_1 = "The input product '";
    public static final String LOG_MSG_INPUT_NOT_EXIST_2 = "' does not exist!";
    public static final String LOG_MSG_PROCESS_PROD_1 = "Processing input product '";
    public static final String LOG_MSG_PROCESS_PROD_2 = "' ...";
    public static final String LOG_MSG_SPATIAL_BINNING = "Performing spatial binning";
    public static final String LOG_MSG_SPATIAL_FINISH = "Applying spatial finish";
    public static final String LOG_MSG_TEMP_BINNING = "Performing temporal aggregation...";
    public static final String LOG_MSG_NO_REQ_BAND = "' does not contain\nthe required band '";
    public static final String LOG_MSG_NO_REQ_FLAG = "' does not contain the flags needed to\ncreate the bitmask '";
    public static final String LOG_MSG_NO_REQ_COORDS = "' is not contained in the desired coordinate range.";
    public static final String LOG_MSG_EXCLUDED = "It is excluded from processing!";

    public static final String MSG_COMPLETED_WITH_WARNINGS = "The processing completed with warnings!";
    public static final String MSG_COMPLETED_SUCCESSFUL = "The processing completed successfully!";
}
