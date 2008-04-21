/*
 * $Id: MosaicConstants.java,v 1.2 2006/12/08 16:09:12 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.processor.mosaic;

import org.esa.beam.framework.dataop.resamp.ResamplingFactory;


public class MosaicConstants {

    //@todo 1 se/nf - check correct english of the entire class

    public static final String PROCESSOR_NAME = "BEAM Mosaic Processor";
    public static final String PROCESSOR_SYMBOLIC_NAME = "beam-mosaic";
    public static final String VERSION_STRING = "2.2.100";
    public static final String COPYRIGHT_INFO = "Copyright (C) 2002-2004 by Brockmann Consult (info@brockmann-consult.de)";

    public static final String REQUEST_TYPE = "MOSAIC";
    public static final String REQUEST_TYPE_MAP_PROJECTION = "MAP_PROJECTION";

    public static final String DEFAULT_OUTPUT_PRODUCT_NAME = "mosaic_out.dim";
    public static final String OUTPUT_PRODUCT_TYPE = "BEAM_MOSAIC";
    public static final String BANDNAME_COUNT = "num_pixels";

//  ****************************
//  *****  User Interface  *****
//  ****************************
    public static final String UI_TITLE = "Mosaic - Processor";

//  *****************************
//  *****  Parameter Units  *****
//  *****************************
    public static final String PARAM_UNIT_DEGREES = "degrees";
    public static final String PARAM_UNIT_METERS = "m";
    public static final String PARAM_UNIT_PIXELS = "pixels";

//  ********************************
//  *****  Request Parameters  *****
//  ********************************
    public static final String PARAM_NAME_UPDATE_MODE = "update_mode";
    public static final String PARAM_LABEL_UPDATE_MODE = "Run in update mode";
    public static final String PARAM_DESCRIPTION_UPDATE_MODE = "Update existing output product with new input products.";
    public static final Boolean PARAM_DEFAULT_VALUE_UPDATE_MODE = false;

    public static final String PARAM_NAME_WEST_LON = "west_lon";
    public static final String PARAM_LABEL_WEST_LON = "West longitude";
    public static final String PARAM_DESCRIPTION_WEST_LON = "Longitude of the upper left corner point.";
    public static final Float PARAM_DEFAULT_VALUE_WEST_LON = -20.f;

    public static final String PARAM_NAME_EAST_LON = "east_lon";
    public static final String PARAM_LABEL_EAST_LON = "East longitude";
    public static final String PARAM_DESCRIPTION_EAST_LON = "Longitude of the lower right corner point.";
    public static final Float PARAM_DEFAULT_VALUE_EAST_LON = 20.f;

    public static final String PARAM_NAME_NORTH_LAT = "north_lat";
    public static final String PARAM_LABEL_NORTH_LAT = "North latitude";
    public static final String PARAM_DESCRIPTION_NORTH_LAT = "Latitude of the upper left corner point.";
    public static final Float PARAM_DEFAULT_VALUE_NORTH_LAT = 60.f;

    public static final String PARAM_NAME_SOUTH_LAT = "south_lat";
    public static final String PARAM_LABEL_SOUTH_LAT = "South latitude";
    public static final String PARAM_DESCRIPTION_SOUTH_LAT = "Latitude of the lower right corner point.";
    public static final Float PARAM_DEFAULT_VALUE_SOUTH_LAT = 40.f;

    public static final String PARAM_NAME_NORTHING = "northing";
    public static final String PARAM_LABEL_NORTHING = "Northing";
    public static final String PARAM_DESCRIPTION_NORTHING = "Northing value.";
    public static final Float PARAM_DEFAULT_VALUE_NORTHING = 0f;

    public static final String PARAM_NAME_EASTING = "easting";
    public static final String PARAM_LABEL_EASTING = "Easting";
    public static final String PARAM_DESCRIPTION_EASTING = "Easting value.";
    public static final Float PARAM_DEFAULT_VALUE_EASTING = 0f;

    public static final String PARAM_NAME_CENTER_LAT = "center_lat";
    public static final String PARAM_LABEL_CENTER_LAT = "Center latitude";
    public static final String PARAM_DESCRIPTION_CENTER_LAT = "Center latitude for the output product.";
    public static final Float PARAM_DEFAULT_VALUE_CENTER_LAT = 50.f;

    public static final String PARAM_NAME_CENTER_LON = "center_lon";
    public static final String PARAM_LABEL_CENTER_LON = "Center longitude";
    public static final String PARAM_DESCRIPTION_CENTER_LON = "Longitude for the output product.";
    public static final Float PARAM_DEFAULT_VALUE_CENTER_LON = 50.f;

    public static final String PARAM_NAME_OUTPUT_WIDTH = "output_width";
    public static final String PARAM_LABEL_OUTPUT_WIDTH = "Output product width";
    public static final String PARAM_DESCRIPTION_OUTPUT_WIDTH = "Raster width for the output product in pixels.";
    public static final Float PARAM_DEFAULT_VALUE_OUTPUT_WIDTH = 50.f;

    public static final String PARAM_NAME_OUTPUT_HEIGHT = "output_height";
    public static final String PARAM_LABEL_OUTPUT_HEIGHT = "Output product height";
    public static final String PARAM_DESCRIPTION_OUTPUT_HEIGHT = "Raster height for the output product in pixels.";
    public static final Float PARAM_DEFAULT_VALUE_OUTPUT_HEIGHT = 50.f;

    public static final String PARAM_NAME_PROJECTION_NAME = "projection_name";
    public static final String PARAM_LABEL_PROJECTION_NAME = "Projection name";
    public static final String PARAM_DESCRIPTION_PROJECTION_NAME = "Map projection of the target product";
    public static final String PARAM_DEFAULT_VALUE_PROJECTION_NAME = "Lambert Conformal Conic";

    public static final String PARAM_NAME_PROJECTION_PARAMETERS = "projection_parameters";
    public static final String PARAM_LABEL_PROJECTION_PARAMETERS = "Projection parameters";/*I18N*/
    public static final String PARAM_DESCRIPTION_PROJECTION_PARAMETERS = "Projection parameter values.";/*I18N*/

    public static final String PARAM_NAME_PIXEL_SIZE_X = "pixel_size_x";
    public static final String PARAM_LABEL_PIXEL_SIZE_X = "Pixel size x";
    public static final String PARAM_DESCRIPTION_PIXEL_SIZE_X = "Pixel size in x-direction of the target product.";
    public static final Float PARAM_DEFAULT_VALUE_PIXEL_SIZE_X = 1200.f;

    public static final String PARAM_NAME_PIXEL_SIZE_Y = "pixel_size_y";
    public static final String PARAM_LABEL_PIXEL_SIZE_Y = "Pixel size y";
    public static final String PARAM_DESCRIPTION_PIXEL_SIZE_Y = "Pixel size in y-direction of the target product.";
    public static final Float PARAM_DEFAULT_VALUE_PIXEL_SIZE_Y = 1200.f;

    public static final String PARAM_NAME_FIT_OUTPUT = "fit_output";
    public static final Boolean PARAM_DEFAULT_VALUE_FIT_OUTPUT = Boolean.FALSE;

    public static final String PARAM_NAME_ORTHORECTIFY_INPUT_PRODUCTS = "orthorectification";
    public static final String PARAM_LABEL_ORTHORECTIFY_INPUT_PRODUCTS = "Orthorectify input products";
    public static final String PARAM_DESCRIPTION_ORTHORECTIFY_INPUT_PRODUCTS = "Orthorectifies all input products if checked";
    public static final Boolean PARAM_DEFAULT_ORTHORECTIFY_INPUT_PRODUCTS = false;

    public static final String PARAM_NAME_ELEVATION_MODEL_FOR_ORTHORECTIFICATION = "orthorectification_dem";
    public static final String PARAM_LABEL_ELEVATION_MODEL_FOR_ORTHORECTIFICATION = "Elevation model";
    public static final String PARAM_DESCRIPTION_ELEVATION_MODEL_FOR_ORTHORECTIFICATION = "The elevation model used by orthorectification";
    public static final String PARAM_DEFAULT_ELEVATION_MODEL_FOR_ORTHORECTIFICATION = "GETASSE30";

    public static final String PARAM_NAME_RESAMPLING_METHOD = "resampling_method";
    public static final String PARAM_LABEL_RESAMPLING_METHOD = "Resampling method";
    public static final String PARAM_DESCRIPTION_RESAMPLING_METHOD = "Resampling method";
    public static final String PARAM_DEFAULT_RESAMPLING_METHOD = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;
    public static final String[] PARAM_VALUESET_RESAMPLING_METHOD = new String[]{
            ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.CUBIC_CONVOLUTION_NAME
    };

    public static final String PARAM_NAME_NO_DATA_VALUE = "no_data_value";

    public static final String PARAM_NAME_CONDITION_OPERATOR = "condition_operator";
    public static final String PARAM_LABEL_CONDITION_OPERATOR = "Combine conditions with";
    public static final String PARAM_DESCRIPTION_CONDITION_OPERATOR = "Contition combination operator";
    public static final String PARAM_DEFAULT_VALUE_CONDITION_OPERATOR = "OR";
    public static final String[] PARAM_VALUESET_CONDITIONS_OPERATOR = new String[]{"OR", "AND"};

    //  this parameter is only used for Batch mode map projection.
    public static final String PARAM_NAME_BANDS = "bands";
    public static final String PARAM_NAME_INCLUDE_TIE_POINT_GRIDS = "include_tie_point_grids";
    public static final Boolean PARAM_DEFAULT_INCLUDE_TIE_POINT_GRIDS = Boolean.TRUE;


//    Processing Parameter
    public static final String PARAM_SUFFIX_EXPRESSION = ".expression";
    public static final String PARAM_SUFFIX_CONDITION = ".condition";
    public static final String PARAM_SUFFIX_OUTPUT = ".output";

//  *********************
//  *****  Logging  *****
//  *********************
    public static final String LOGGER_NAME = "beam.processor.mosaic";
    public static final String DEFAULT_LOG_PREFIX = "mosaic";
//    ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME
//    ProcessorConstants.LOG_PREFIX_PARAM_NAME

    // Support for PixelGeoCoding
    public static final String PARAM_NAME_GEOCODING_LATITUDES = "geocoding_latitudes";
    public static final String PARAM_NAME_GEOCODING_LONGITUDES = "geocoding_longitudes";
    public static final String PARAM_NAME_GEOCODING_VALID_MASK = "geocoding_valid_mask";
    public static final String PARAM_NAME_GEOCODING_SEARCH_RADIUS = "geocoding_search_radius";
    public static final int DEFAULT_GEOCODING_SEARCH_RADIUS = 7;
}
