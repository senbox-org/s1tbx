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
package org.esa.snap.core.dataio.dimap;

/**
 * This class defines some frequently used constants for BEAM DIMAP products.
 * <p>
 * The BEAM-DIMAP version history is provided in the API doc of the {@link DimapProductWriterPlugIn}.
 *
 * @author Sabine Embacher
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public final class DimapProductConstants {

    public static final String DIMAP_FORMAT_NAME = "BEAM-DIMAP";
    /**
     * BEAM-Dimap XML-File extension
     */
    public static final String DIMAP_HEADER_FILE_EXTENSION = ".dim";
    /**
     * BEAM-Dimap data directory extension
     */
    public static final String DIMAP_DATA_DIRECTORY_EXTENSION = ".data";
    public static final String IMAGE_FILE_EXTENSION = ".img";  /* ENVI specific */
    public static final String TIE_POINT_GRID_DIR_NAME = "tie_point_grids";

    /**
     * BEAM-DIMAP version number.
     * <p>
     * Important note: If you change this number, update the BEAM-DIMAP version history given at {@link DimapProductWriterPlugIn}.
     */
    public static final String DIMAP_CURRENT_VERSION = "2.12.1";

    // BEAM-Dimap default text
    public static final String DIMAP_METADATA_PROFILE = "BEAM-DATAMODEL-V1";
    public static final String DIMAP_DATASET_SERIES = "BEAM-PRODUCT";
    public static final String DATASET_PRODUCER_NAME = " ";
    //    public final static String DATASET_PRODUCER_NAME = "Brockmann-Consult | Phone +49 (04152) 889 300";
    public static final String DATA_FILE_FORMAT = "ENVI";
    public static final String DATA_FILE_FORMAT_DESCRIPTION = "ENVI File Format";
    public static final String DATA_FILE_ORGANISATION = "BAND_SEPARATE";

    // BEAM-Dimap document root tag
    public static final String TAG_ROOT = "Dimap_Document";

    // BEAM-Dimap metadata ID tags
    public static final String TAG_METADATA_ID = "Metadata_Id";
    public static final String TAG_METADATA_FORMAT = "METADATA_FORMAT";
    public static final String TAG_METADATA_PROFILE = "METADATA_PROFILE";

    // BEAM-Dimap production tags
    public static final String TAG_PRODUCTION = "Production";
    public static final String TAG_DATASET_PRODUCER_NAME = "DATASET_PRODUCER_NAME";
    public static final String TAG_DATASET_PRODUCER_URL = "DATASET_PRODUCER_URL";
    public static final String TAG_DATASET_PRODUCTION_DATE = "DATASET_PRODUCTION_DATE";
    public static final String TAG_QUICKLOOK_BAND_NAME = "QUICKLOOK_BAND_NAME";
    public static final String TAG_JOB_ID = "JOB_ID";
    public static final String TAG_PRODUCT_TYPE = "PRODUCT_TYPE";
    public static final String TAG_PRODUCT_INFO = "PRODUCT_INFO";
    public static final String TAG_PROCESSING_REQUEST = "PROCESSING_REQUEST";
    public static final String TAG_REQUEST = "Request";
    public static final String TAG_PARAMETER = "Parameter";
    public static final String TAG_INPUTPRODUCT = "InputProduct";
    public static final String TAG_OUTPUTPRODUCT = "OutputProduct";
    public static final String TAG_PRODUCT_SCENE_RASTER_START_TIME = "PRODUCT_SCENE_RASTER_START_TIME";
    public static final String TAG_PRODUCT_SCENE_RASTER_STOP_TIME = "PRODUCT_SCENE_RASTER_STOP_TIME";
    public static final String TAG_OLD_SCENE_RASTER_START_TIME = "SENSING_START";
    public static final String TAG_OLD_SCENE_RASTER_STOP_TIME = "SENSING_STOP";

    // BEAM-Dimap geocoding tags
    public static final String TAG_COORDINATE_REFERENCE_SYSTEM = "Coordinate_Reference_System";
    public static final String TAG_GEOCODING_TIE_POINT_GRIDS = "Geocoding_Tie_Point_Grids";
    public static final String TAG_GEOPOSITION_POINTS = "Geoposition_Points";
    public static final String TAG_ORIGINAL_GEOCODING = "Original_Geocoding";
    public static final String TAG_INTERPOLATION_METHOD = "INTERPOLATION_METHOD";
    public static final String TAG_TIE_POINT_GRID_NAME_LAT = "TIE_POINT_GRID_NAME_LAT";
    public static final String TAG_TIE_POINT_GRID_NAME_LON = "TIE_POINT_GRID_NAME_LON";
    public static final String TAG_GEOCODING_MAP = "Geocoding_Map";
    public static final String TAG_GEOCODING_MAP_INFO = "MAP_INFO";
    public static final String TAG_LATITUDE_BAND = "LATITUDE_BAND";
    public static final String TAG_LONGITUDE_BAND = "LONGITUDE_BAND";
    public static final String TAG_VALID_MASK_EXPRESSION = "VALID_MASK_EXPRESSION";
    public static final String TAG_SEARCH_RADIUS = "SEARCH_RADIUS";
    public static final String TAG_PIXEL_POSITION_ESTIMATOR = "Pixel_Position_Estimator";
    public static final String TAG_WKT = "WKT";
    //This Tag is used for geo-coding support and multi size support
    public static final String TAG_IMAGE_TO_MODEL_TRANSFORM = "IMAGE_TO_MODEL_TRANSFORM";

    //  -since version 2.0.0
    public static final String TAG_HORIZONTAL_CS_TYPE = "HORIZONTAL_CS_TYPE";


    public static final String TAG_MAP_INFO_PIXEL_X = "PIXEL_X";
    public static final String TAG_MAP_INFO_PIXEL_Y = "PIXEL_Y";
    public static final String TAG_MAP_INFO_EASTING = "EASTING";
    public static final String TAG_MAP_INFO_NORTHING = "NORTHING";
    public static final String TAG_MAP_INFO_ORIENTATION = "ORIENTATION";
    public static final String TAG_MAP_INFO_PIXELSIZE_X = "PIXELSIZE_X";
    public static final String TAG_MAP_INFO_PIXELSIZE_Y = "PIXELSIZE_Y";
    public static final String TAG_MAP_INFO_NODATA_VALUE = "NODATA_VALUE";
    public static final String TAG_MAP_INFO_MAPUNIT = "MAPUNIT";
    public static final String TAG_MAP_INFO_ORTHORECTIFIED = "ORTHORECTIFIED";
    public static final String TAG_MAP_INFO_ELEVATION_MODEL = "ELEVATION_MODEL";
    public static final String TAG_MAP_INFO_SCENE_FITTED = "SCENE_FITTED";
    public static final String TAG_MAP_INFO_SCENE_WIDTH = "SCENE_WIDTH";
    public static final String TAG_MAP_INFO_SCENE_HEIGHT = "SCENE_HEIGHT";
    public static final String TAG_MAP_INFO_RESAMPLING = "RESAMPLING";

    public static final String TAG_GEOPOSITION = "Geoposition";
    public static final String TAG_GEOPOSITION_INSERT = "Geoposition_Insert";
    public static final String TAG_ULX_MAP = "ULXMAP";
    public static final String TAG_ULY_MAP = "ULYMAP";
    public static final String TAG_X_DIM = "XDIM";
    public static final String TAG_Y_DIM = "YDIM";
    public static final String TAG_SIMPLIFIED_LOCATION_MODEL = "Simplified_Location_Model";
    public static final String TAG_DIRECT_LOCATION_MODEL = "Direct_Location_Model";
    public static final String TAG_LC_LIST = "lc_List";
    public static final String TAG_LC = "lc";
    public static final String TAG_PC_LIST = "pc_List";
    public static final String TAG_PC = "pc";
    public static final String TAG_REVERSE_LOCATION_MODEL = "Reverse_Location_Model";
    public static final String TAG_IC_LIST = "ic_List";
    public static final String TAG_IC = "ic";
    public static final String TAG_JC_LIST = "jc_List";
    public static final String TAG_JC = "jc";

    //   - since version 1.4.0
    public static final String TAG_GEO_TABLES = "GEO_TABLES";
    public static final String TAG_HORIZONTAL_CS = "Horizontal_CS";
    public static final String TAG_HORIZONTAL_CS_NAME = "HORIZONTAL_CS_NAME";
    public static final String TAG_GEOGRAPHIC_CS = "Geographic_CS";
    public static final String TAG_GEOGRAPHIC_CS_NAME = "GEOGRAPHIC_CS_NAME";
    public static final String TAG_HORIZONTAL_DATUM = "Horizontal_Datum";
    public static final String TAG_HORIZONTAL_DATUM_NAME = "HORIZONTAL_DATUM_NAME";
    public static final String TAG_ELLIPSOID = "Ellipsoid";
    public static final String TAG_ELLIPSOID_NAME = "ELLIPSOID_NAME";
    public static final String TAG_ELLIPSOID_PARAMETERS = "Ellipsoid_Parameters";
    public static final String TAG_ELLIPSOID_MAJ_AXIS = "ELLIPSOID_MAJ_AXIS";
    public static final String TAG_ELLIPSOID_MIN_AXIS = "ELLIPSOID_MIN_AXIS";
    public static final String TAG_PROJECTION = "Projection";
    public static final String TAG_PROJECTION_NAME = "NAME";
    public static final String TAG_PROJECTION_CT_METHOD = "Projection_CT_Method";
    public static final String TAG_PROJECTION_CT_NAME = "PROJECTION_CT_NAME";
    public static final String TAG_PROJECTION_PARAMETERS = "Projection_Parameters";
    public static final String TAG_PROJECTION_PARAMETER = "Projection_Parameter";
    public static final String TAG_PROJECTION_PARAMETER_NAME = "PROJECTION_PARAMETER_NAME";
    public static final String TAG_PROJECTION_PARAMETER_VALUE = "PROJECTION_PARAMETER_VALUE";

    // BEAM-Dimap dataset id tags
    public static final String TAG_DATASET_ID = "Dataset_Id";
    public static final String TAG_DATASET_INDEX = "DATASET_INDEX";
    public static final String TAG_DATASET_SERIES = "DATASET_SERIES";
    public static final String TAG_DATASET_NAME = "DATASET_NAME";
    public static final String TAG_DATASET_DESCRIPTION = "DATASET_DESCRIPTION";
    public static final String TAG_DATASET_AUTO_GROUPING = "DATASET_AUTO_GROUPING";
    public static final String TAG_COPYRIGHT = "COPYRIGHT";
    public static final String TAG_COUNTRY_NAME = "COUNTRY_NAME";
    public static final String TAG_COUNTRY_CODE = "COUNTRY_CODE";
    public static final String TAG_DATASET_LOCATION = "DATASET_LOCATION";
    public static final String TAG_DATASET_TN_PATH = "DATASET_TN_PATH";
    public static final String TAG_DATASET_TN_FORMAT = "DATASET_TN_FORMAT";
    public static final String TAG_DATASET_QL_PATH = "DATASET_QL_PATH";
    public static final String TAG_DATASET_QL_FORMAT = "DATASET_QL_FORMAT";

    // BEAM_Dimap dataset use tags
    public static final String TAG_DATASET_USE = "Dataset_Use";
    public static final String TAG_DATASET_COMMENTS = "DATASET_COMMENTS";

    // BEAM-Dimap flag coding tags
    public static final String TAG_FLAG_CODING = "Flag_Coding";
    public static final String TAG_FLAG = "Flag";
    public static final String TAG_FLAG_NAME = "Flag_Name";
    public static final String TAG_FLAG_INDEX = "Flag_Index";
    public static final String TAG_FLAG_DESCRIPTION = "Flag_description";

    // BEAM-Dimap index coding tags
    public static final String TAG_INDEX_CODING = "Index_Coding";
    public static final String TAG_INDEX = "Index";
    public static final String TAG_INDEX_NAME = "INDEX_NAME";
    public static final String TAG_INDEX_VALUE = "INDEX_VALUE";
    public static final String TAG_INDEX_DESCRIPTION = "INDEX_DESCRIPTION";

    // BEAM-Dimap raster dimension tags
    public static final String TAG_RASTER_DIMENSIONS = "Raster_Dimensions";
    public static final String TAG_NCOLS = "NCOLS";
    public static final String TAG_NROWS = "NROWS";
    public static final String TAG_NBANDS = "NBANDS";

    // BEAM-Dimap tie point grid tags
    public static final String TAG_TIE_POINT_GRIDS = "Tie_Point_Grids";
    public static final String TAG_TIE_POINT_NUM_TIE_POINT_GRIDS = "NUM_TIE_POINT_GRIDS";
    public static final String TAG_TIE_POINT_GRID_INFO = "Tie_Point_Grid_Info";
    public static final String TAG_TIE_POINT_GRID_INDEX = "TIE_POINT_GRID_INDEX";
    public static final String TAG_TIE_POINT_DESCRIPTION = "TIE_POINT_DESCRIPTION";
    public static final String TAG_TIE_POINT_PHYSICAL_UNIT = "PHYSICAL_UNIT";
    public static final String TAG_TIE_POINT_GRID_NAME = "TIE_POINT_GRID_NAME";
    public static final String TAG_TIE_POINT_DATA_TYPE = "DATA_TYPE";
    public static final String TAG_TIE_POINT_NCOLS = "NCOLS";
    public static final String TAG_TIE_POINT_NROWS = "NROWS";
    public static final String TAG_TIE_POINT_OFFSET_X = "OFFSET_X";
    public static final String TAG_TIE_POINT_OFFSET_Y = "OFFSET_Y";
    public static final String TAG_TIE_POINT_STEP_X = "STEP_X";
    public static final String TAG_TIE_POINT_STEP_Y = "STEP_Y";
    public static final String TAG_TIE_POINT_CYCLIC = "CYCLIC";

    // BEAM-Dimap data access tags
    public static final String TAG_DATA_ACCESS = "Data_Access";
    public static final String TAG_DATA_FILE_FORMAT = "DATA_FILE_FORMAT";
    public static final String TAG_DATA_FILE_FORMAT_DESC = "DATA_FILE_FORMAT_DESC";
    public static final String TAG_DATA_FILE_ORGANISATION = "DATA_FILE_ORGANISATION";
    public static final String TAG_DATA_FILE = "Data_File";
    public static final String TAG_DATA_FILE_PATH = "DATA_FILE_PATH";
    public static final String TAG_BAND_INDEX = "BAND_INDEX";
    public static final String TAG_TIE_POINT_GRID_FILE = "Tie_Point_Grid_File";
    public static final String TAG_TIE_POINT_GRID_FILE_PATH = "TIE_POINT_GRID_FILE_PATH";

    // BEAM-Dimap image display tags
    public static final String TAG_IMAGE_DISPLAY = "Image_Display";
    public static final String TAG_BAND_STATISTICS = "Band_Statistics";
    public static final String TAG_STX_MIN = "STX_MIN";
    public static final String TAG_STX_MAX = "STX_MAX";
    public static final String TAG_STX_MEAN = "STX_MEAN";
    public static final String TAG_STX_STDDEV = "STX_STD_DEV";
    public static final String TAG_STX_LEVEL = "STX_RES_LEVEL";
    public static final String TAG_STX_LIN_MIN = "STX_LIN_MIN";
    public static final String TAG_STX_LIN_MAX = "STX_LIN_MAX";
    public static final String TAG_HISTOGRAM = "HISTOGRAM";
    public static final String TAG_NUM_COLORS = "NUM_COLORS";
    public static final String TAG_COLOR_PALETTE_POINT = "Color_Palette_Point";
    public static final String TAG_SAMPLE = "SAMPLE";
    public static final String TAG_LABEL = "LABEL";
    public static final String TAG_COLOR = "COLOR";
    public static final String TAG_GAMMA = "GAMMA";
    public static final String TAG_NO_DATA_COLOR = "NO_DATA_COLOR";
    public static final String TAG_HISTOGRAM_MATCHING = "HISTOGRAM_MATCHING";
    public static final String TAG_BITMASK_OVERLAY = "Bitmask_Overlay";
    public static final String TAG_BITMASK = "BITMASK";
    public static final String TAG_ROI_DEFINITION = "ROI_Definition";
    public static final String TAG_ROI_ONE_DIMENSIONS = "ROI_ONE_DIMENSIONS";
    public static final String TAG_VALUE_RANGE_MAX = "VALUE_RANGE_MAX";
    public static final String TAG_VALUE_RANGE_MIN = "VALUE_RANGE_MIN";
    public static final String TAG_BITMASK_ENABLED = "BITMASK_ENABLED";
    public static final String TAG_INVERTED = "INVERTED";
    public static final String TAG_OR_COMBINED = "OR_COMBINED";
    public static final String TAG_SHAPE_ENABLED = "SHAPE_ENABLED";
    public static final String TAG_SHAPE_FIGURE = "Shape_Figure";
    public static final String TAG_VALUE_RANGE_ENABLED = "VALUE_RANGE_ENABLED";
    public static final String TAG_PATH_SEG = "SEGMENT";
    public static final String TAG_PIN_USE_ENABLED = "PIN_USE_ENABLED";
    public static final String TAG_MASK_USAGE = "Mask_Usage";
    public static final String TAG_ROI = "ROI";
    public static final String TAG_OVERLAY = "OVERLAY";

    // BEAM-Dimap image interpretation tags
    public static final String TAG_IMAGE_INTERPRETATION = "Image_Interpretation";
    public static final String TAG_SPECTRAL_BAND_INFO = "Spectral_Band_Info";
    public static final String TAG_VIRTUAL_BAND_INFO = "Virtual_Band_Info";
    public static final String TAG_BAND_DESCRIPTION = "BAND_DESCRIPTION";
    public static final String TAG_PHYSICAL_GAIN = "PHYSICAL_GAIN";
    public static final String TAG_PHYSICAL_BIAS = "PHYSICAL_BIAS";
    public static final String TAG_PHYSICAL_UNIT = "PHYSICAL_UNIT";
    public static final String TAG_BAND_NAME = "BAND_NAME";
    public static final String TAG_BAND_RASTER_WIDTH = "BAND_RASTER_WIDTH";
    public static final String TAG_BAND_RASTER_HEIGHT = "BAND_RASTER_HEIGHT";
    public static final String TAG_DATA_TYPE = "DATA_TYPE";
    public static final String TAG_SOLAR_FLUX = "SOLAR_FLUX";
    public static final String TAG_SPECTRAL_BAND_INDEX = "SPECTRAL_BAND_INDEX";
    public static final String TAG_SOLAR_FLUX_UNIT = "SOLAR_FLUX_UNIT";
    public static final String TAG_BANDWIDTH = "BANDWIDTH";
    public static final String TAG_BAND_WAVELEN = "BAND_WAVELEN";
    public static final String TAG_WAVELEN_UNIT = "WAVELEN_UNIT";
    public static final String TAG_FLAG_CODING_NAME = "FLAG_CODING_NAME";
    public static final String TAG_INDEX_CODING_NAME = "INDEX_CODING_NAME";
    public static final String TAG_SCALING_FACTOR = "SCALING_FACTOR";
    public static final String TAG_SCALING_OFFSET = "SCALING_OFFSET";
    public static final String TAG_SCALING_LOG_10 = "LOG10_SCALED";
    public static final String TAG_VALID_MASK_TERM = "VALID_MASK_TERM";
    public static final String TAG_NO_DATA_VALUE_USED = "NO_DATA_VALUE_USED";
    public static final String TAG_NO_DATA_VALUE = "NO_DATA_VALUE";

    //Ancillary support
    public static final String TAG_ANCILLARY_RELATION = "ANCILLARY_RELATION";
    public static final String TAG_ANCILLARY_VARIABLE = "ANCILLARY_VARIABLE";

    //Virtual bands support
    public static final String TAG_VIRTUAL_BAND = "VIRTUAL_BAND";
    public static final String TAG_VIRTUAL_BAND_CHECK_INVALIDS = "CHECK_INVALIDS";
    public static final String TAG_VIRTUAL_BAND_EXPRESSION = "EXPRESSION";
    public static final String TAG_VIRTUAL_BAND_INVALID_VALUE = "INVALID_VALUE";
    public static final String TAG_VIRTUAL_BAND_USE_INVALID_VALUE = "USE_INVALID_VALUE";
    public static final String TAG_VIRTUAL_BAND_WRITE_DATA = "WRITE_DATA";

    // Filter bands support -- version 1.0
    @Deprecated
    public static final String TAG_FILTER_SUB_WINDOW_WIDTH = "FILTER_SUB_WINDOW_WIDTH";
    @Deprecated
    public static final String TAG_FILTER_SUB_WINDOW_HEIGHT = "FILTER_SUB_WINDOW_HEIGHT";

    // Filter bands support -- versions 1.0, 1.1
    public static final String TAG_FILTER_BAND_INFO = "Filter_Band_Info";
    public static final String TAG_FILTER_SOURCE = "FILTER_SOURCE";
    public static final String TAG_FILTER_KERNEL = "Filter_Kernel";
    public static final String TAG_FILTER_OP_TYPE = "FILTER_OP_TYPE";
    public static final String TAG_FILTER_SUB_WINDOW_SIZE = "FILTER_SUB_WINDOW_SIZE";
    public static final String TAG_FILTER_OPERATOR_CLASS_NAME = "FILTER_OPERATOR_CLASS_NAME";

    // Kernel support
    public static final String TAG_KERNEL_HEIGHT = "KERNEL_HEIGHT";
    public static final String TAG_KERNEL_WIDTH = "KERNEL_WIDTH";
    public static final String TAG_KERNEL_X_ORIGIN = "KERNEL_X_ORIGIN"; // new in 1.2
    public static final String TAG_KERNEL_Y_ORIGIN = "KERNEL_Y_ORIGIN"; // new in 1.2
    public static final String TAG_KERNEL_FACTOR = "KERNEL_FACTOR";
    public static final String TAG_KERNEL_DATA = "KERNEL_DATA";

    // BEAM-Dimap dataset sources tags
    public static final String TAG_DATASET_SOURCES = "Dataset_Sources";
    public static final String TAG_SOURCE_INFORMATION = "Source_Information";
    public static final String TAG_SOURCE_ID = "SOURCE_ID";
    public static final String TAG_SOURCE_TYPE = "SOURCE_TYPE";
    public static final String TAG_SOURCE_DESCRIPTION = "SOURCE_DESCRIPTION";
    public static final String TAG_SOURCE_FRAME = "Source_Frame";
    public static final String TAG_VERTEX = "Vertex";
    public static final String TAG_FRAME_LON = "FRAME_LON";
    public static final String TAG_FRAME_LAT = "FRAME_LAT";
    public static final String TAG_FRAME_X = "FRAME_X";
    public static final String TAG_FRAME_Y = "FRAME_Y";
    public static final String TAG_SCENE_SOURCE = "Scene_Source";
    public static final String TAG_MISSION = "MISSION";
    public static final String TAG_INSTRUMENT = "INSTRUMENT";
    public static final String TAG_IMAGING_MODE = "IMAGING_MODE";
    public static final String TAG_IMAGING_DATE = "IMAGING_DATE";
    public static final String TAG_IMAGING_TIME = "IMAGING_TIME";
    public static final String TAG_GRID_REFERENCE = "GRID_REFERENCE";
    public static final String TAG_SCENE_RECTIFICATION_ELEV = "SCENE_RECTIFICATION_ELEV";
    public static final String TAG_INCIDENCE_ANGLE = "INCIDENCE_ANGLE";
    public static final String TAG_THEORETICAL_RESOLUTION = "THEORETICAL_RESOLUTION";
    public static final String TAG_SUN_AZIMUTH = "SUN_AZIMUTH";
    public static final String TAG_SUN_ELEVATION = "SUN_ELEVATION";
    public static final String TAG_METADATA_ELEMENT = "MDElem";
    public static final String TAG_METADATA_VALUE = "VALUE";
    public static final String TAG_METADATA_ATTRIBUTE = "MDATTR";

    // BEAM-Dimap mask definition tags
    public static final String TAG_MASKS = "Masks";
    public static final String TAG_MASK = "Mask";
    public static final String TAG_NAME = "NAME";
    public static final String TAG_DESCRIPTION = "DESCRIPTION";
    public static final String TAG_TRANSPARENCY = "TRANSPARENCY";
    public static final String TAG_MASK_RASTER_WIDTH = "MASK_RASTER_WIDTH";
    public static final String TAG_MASK_RASTER_HEIGHT = "MASK_RASTER_HEIGHT";

    // BandMathMask
    public static final String TAG_EXPRESSION = "EXPRESSION";
    // RangeMask
    public static final String TAG_MINIMUM = "MINIMUM";
    public static final String TAG_MAXIMUM = "MAXIMUM";
    public static final String TAG_RASTER = "RASTER";

    // BEAM-Dimap bitmask definition tags
    public static final String TAG_BITMASK_DEFINITIONS = "Bitmask_Definitions";
    public static final String TAG_BITMASK_DEFINITION = "Bitmask_Definition";
    public static final String TAG_BITMASK_DESCRIPTION = TAG_DESCRIPTION;
    public static final String TAG_BITMASK_EXPRESSION = TAG_EXPRESSION;
    public static final String TAG_BITMASK_COLOR = TAG_COLOR;
    public static final String TAG_BITMASK_TRANSPARENCY = TAG_TRANSPARENCY;


    // BEAM-Dimap placemark tags
    public static final String TAG_PLACEMARK = "Placemark";
    public static final String TAG_PLACEMARK_LABEL = "LABEL";
    public static final String TAG_PLACEMARK_DESCRIPTION = "DESCRIPTION";
    public static final String TAG_PLACEMARK_LATITUDE = "LATITUDE";
    public static final String TAG_PLACEMARK_LONGITUDE = "LONGITUDE";
    public static final String TAG_PLACEMARK_PIXEL_X = "PIXEL_X";
    public static final String TAG_PLACEMARK_PIXEL_Y = "PIXEL_Y";
    public static final String TAG_PLACEMARK_STYLE_CSS = "STYLE_CSS";
    /**
     * @deprecated since SNAP 2.0
     */
    @Deprecated
    public static final String TAG_PLACEMARK_FILL_COLOR = "FillColor";
    /**
     * @deprecated since SNAP 2.0
     */
    @Deprecated
    public static final String TAG_PLACEMARK_OUTLINE_COLOR = "OutlineColor";

    // BEAM-Dimap pin tags
    public static final String TAG_PIN_GROUP = "Pin_Group";
    public static final String TAG_PIN = "Pin";

    // BEAM-Dimap gcp tags
    public static final String TAG_GCP_GROUP = "Gcp_Group";

    // attribute
    public static final String ATTRIB_RED = "red";
    public static final String ATTRIB_GREEN = "green";
    public static final String ATTRIB_BLUE = "blue";
    public static final String ATTRIB_ALPHA = "alpha";
    public static final String ATTRIB_NAMES = "names";
    public static final String ATTRIB_DESCRIPTION = "desc";
    public static final String ATTRIB_UNIT = "unit";
    public static final String ATTRIB_MODE = "mode";
    public static final String ATTRIB_TYPE = "type";
    public static final String ATTRIB_ELEMS = "elems";
    public static final String ATTRIB_NAME = "name";
    public static final String ATTRIB_VERSION = "version";
    public static final String ATTRIB_HREF = "href";
    public static final String ATTRIB_VALUE = "value";
    public static final String ATTRIB_ORDER = "order";
    public static final String ATTRIB_INDEX = "index";
    public static final String ATTRIB_BAND_TYPE = "bandType";
}
