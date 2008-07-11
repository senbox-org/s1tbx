/*
 * $Id: DimapProductConstants.java,v 1.2 2007/02/19 14:02:58 marcoz Exp $
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
package org.esa.beam.dataio.dimap;

/**
 * This class defines some frequently used constants for BEAM DIMAP products.
 *
 * @author Sabine Embacher
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public final class DimapProductConstants {

    public final static String DIMAP_FORMAT_NAME = "BEAM-DIMAP";
    /**
     * BEAM-Dimap XML-File extension
     */
    public final static String DIMAP_HEADER_FILE_EXTENSION = ".dim";
    /**
     * BEAM-Dimap data directory extension
     */
    public final static String DIMAP_DATA_DIRECTORY_EXTENSION = ".data";
    public static final String IMAGE_FILE_EXTENSION = ".img";  /* ENVI specific */
    public static final String TIE_POINT_GRID_DIR_NAME = "tie_point_grids";

    // BEAM-Dimap version
    /**
     * BEAM-Dimap versions
     * <table>
     * <tr><th>Version</th><th>Comment</th></tr>
     * <tr> <td valign="top">1.0</td> <td>initial version BEAM version 1.0</td> </tr>
     * <tr> <td valign="top">1.0.1</td>
     * <td>product.description now saved to &lt;DATASET_COMMENTS&gt; inside &lt;Dataset_use&gt;</td> </tr>
     * <tr> <td valign="top">1.0.2</td>
     * <td> MapInfo modified:<br>
     * SceneWidth and SceneHeight added<br>
     * affected tag:<br>
     * &lt;MAP_INFO&gt;
     * inside &lt;Geocoding_Map&gt;
     * inside &lt;Coordinate_Reference_System&gt; </td> </tr>
     * <tr> <td valign="top">1.1.0</td>
     * <td> Band modified:<br>
     * ScalingFactor, ScalingOffset and Log10Scaled properties added<br>
     * affected tag:<br>
     * &lt;Spectral_Band_Info&gt;
     * inside &lt;Image_Interpretation&gt;<br>
     * new tags added:<br>
     * &lt;SCALING_FACTOR&gt;
     * &lt;SCALING_OFFSET&gt;
     * &lt;LOG_10_SCALED&gt;<br> </td> </tr>
     * <tr> <td valign="top">1.1.1</td>
     * <td> Supports now <code>VirtualBand</code>.<br>
     * affected tag:<br>
     * &lt;Spectral_Band_Info&gt;
     * inside &lt;Image_Interpretation&gt;<br>
     * new tags added if <code>Band</code> was a <code>VirtualBand</code>:<br>
     * &lt;VIRTUAL_BAND&gt;
     * &lt;CHECK_INVALIDS&gt;
     * &lt;EXPRESSION&gt;
     * &lt;INVALID_VALUE&gt;
     * &lt;USE_INVALID_VALUE&gt;<br> </td> </tr>
     * <tr> <td valign="top">1.1.2</td>
     * <td> Supports now <code>cyclic TiePointGrid</code>.<br>
     * affected tag:<br>
     * &lt;Tie_Point_Grid_Info&gt;
     * inside &lt;Tie_Point_Grids&gt;<br>
     * new tag added:<br>
     * &lt;CYCLIC&gt<br> </td> </tr>
     * <tr> <td valign="top">1.2.0</td>
     * <td> Now supports <code>Pin</code>.<br>
     * new tag added:<br>
     * &lt;Pin&gt;<br>
     * and inside:<br>
     * &lt;DESCRIPTION&gt;
     * &lt;LATITUDE&gt;
     * &lt;LONGITUDE&gt;<br> </td> </tr>
     * <tr> <td valign="top">1.3.0</td>
     * <td> Now supports <code>GAMMA</code> for contrast stretch.<br>
     * affected tag:<br>
     * &lt;Band_Statistics&gt;
     * inside &lt;Image_Display&gt;<br>
     * new tags added:<br>
     * &lt;GAMMA&gt<br><br>
     * Now supports <code>ValidMasks</code> for bands.<br>
     * affected tag:<br>
     * &lt;Spectral_Band_Info&gt;
     * inside &lt;Image_Interpretation&gt;<br>
     * new tags added:<br>
     * &lt;VALID_MASK_TERM&gt<br> </td> </tr>
     * <tr> <td valign="top">1.4.0</td>
     * <td> Now the MapGeocoding was written in a new style
     * <pre>
     * &lt;Coordinate_Reference_System&gt;
     *   &lt;GEO_TABLES version="1.0"&gt;CUSTOM&lt;/GEO_TABLES&gt; // notMandatory
     *   &lt;Horizontal_CS&gt;
     *     &lt;HORIZONTAL_CS_TYPE&gt;PROJECTED&lt;/HORIZONTAL_CS_TYPE&gt; // notMandatory
     *     &lt;HORIZONTAL_CS_NAME&gt;" + projectionName + "&lt;/HORIZONTAL_CS_NAME&gt; // notMandatory
     *     &lt;Geographic_CS&gt;
     *       &lt;GEOGRAPHIC_CS_NAME&gt;" + projectionName + "&lt;/GEOGRAPHIC_CS_NAME&gt; // notMandatory
     *       &lt;Horizontal_Datum&gt;
     *         &lt;HORIZONTAL_DATUM_NAME&gt;" + datumName + "&lt;/HORIZONTAL_DATUM_NAME&gt;
     *         &lt;Ellipsoid&gt;
     *           &lt;ELLIPSOID_NAME&gt;" + ellipsoidName + "&lt;/ELLIPSOID_NAME&gt;
     *           &lt;Ellipsoid_Parameters&gt;
     *             &lt;ELLIPSOID_MAJ_AXIS unit="M"&gt;" + semiMajor + "&lt;/ELLIPSOID_MAJ_AXIS&gt;
     *             &lt;ELLIPSOID_MIN_AXIS unit="M"&gt;" + semiMinor + "&lt;/ELLIPSOID_MIN_AXIS&gt;
     *           &lt;/Ellipsoid_Parameters&gt;
     *         &lt;/Ellipsoid&gt;
     *       &lt;/Horizontal_Datum&gt;
     *     &lt;/Geographic_CS&gt;
     *     &lt;Projection&gt;
     *       &lt;NAME&gt;" + projectionName + "&lt;/NAME&gt;
     *       &lt;Projection_CT_Method&gt;
     *         &lt;PROJECTION_CT_NAME&gt;" + projectionTypeId + "&lt;/PROJECTION_CT_NAME&gt;
     *         &lt;Projection_Parameters&gt;
     *           &lt;!-- one or more &lt;Projection_Parameter&gt; elements --&gt;
     *           &lt;Projection_Parameter&gt;
     *             &lt;PROJECTION_PARAMETER_NAME&gt;" + paramName + "&lt;/PROJECTION_PARAMETER_NAME&gt; // notMandatory
     *             &lt;PROJECTION_PARAMETER_VALUE unit="" + parameterUnit + ""&gt;" + parameterValues + "&lt;/PROJECTION_PARAMETER_VALUE&gt;
     *           &lt;/Projection_Parameter&gt;
     *           ...
     *         &lt;/Projection_Parameters&gt;
     *       &lt;/Projection_CT_Method&gt;
     *     &lt;/Projection&gt;
     *     &lt;MAP_INFO&gt;" + mapInfo.toString + "&lt;/MAP_INFO&gt;
     *     &lt;!-- mapInfo.toString looks linke this: --&gt;
     *     &lt;!-- projectionName, pixelX, pixelY, easting, northing, pixelSizeX, pixelSizeY, datumName, units=mapUnit, sceneWidth, sceneHeight --&gt;
     *     &lt;!-- For example --&gt;
     *     &lt;!-- Lambert Conformal Conic, 0.0, 0.0, -1654216.0, 2025576.8, 5000.0, 5000.0, WGS-84, units=meter, 625, 1023 --&gt;
     *   &lt;/Horizontal_CS&gt;
     * &lt;/Coordinate_Reference_System&gt;
     * </pre>
     * </td> </tr>
     * <tr> <td valign="top">1.4.1</td>
     * <td> Now supports <code>SceneRasterSensingStart</code> and <code>SceneRasterSensingStop</code> of product.<br>
     * Affected tag:<br>
     * &lt;Production&gt;<br>
     * new tags added:<br>
     * &lt;PRODUCT_SCENE_RASTER_START_TIME&gt &lt;PRODUCT_SCENE_RASTER_STOP_TIME&gt<br> </td> </tr>
     * <tr> <td valign="top">1.4.2</td>
     * <td> Since BEAM version 3.2 {@link org.esa.beam.framework.datamodel.RasterDataNode RasterDataNode} supports the
     * <code>NoDataValue</code>.<br>
     * Affected tag:<br>
     * &lt;Spectral_Band_Info&gt; inside of &lt;Image_Interpretation&gt;<br>
     * Following tags are replaced:<br>
     * &lt;INVALID_VALUE&gt; replaced by &lt;NO_DATA_VALUE&gt;<br>
     * &lt;USE_INVALID_VALUE&gt; replaced by &lt;NO_DATA_VALUE_USED&gt;<br> </td> </tr>
     * <tr> <td valign="top">2.0.0</td>
     * <td> Since version 3.5 BEAM supports {@link org.esa.beam.framework.datamodel.FXYGeoCoding}.<br>
     * New tags:<br>
     * <pre>
     * &lt;Geoposition&gt;
     * &lt;!-- Tha tag BAND_INDEX is only used if the geo-coding is writen per band --&gt;
     * &lt;BAND_INDEX&gt; index &lt;BAND_INDEX&gt;
     *  &lt;Geoposition_Insert&gt;
     *      &lt;ULXMAP unit="M"&gt; referencePixelX &lt;/ULXMAP&gt;
     *      &lt;ULYMAP unit="M"&gt; referencePixelY &lt;/ULYMAP&gt;
     *      &lt;XDIM unit="M"&gt; pixelSizeX &lt;/XDIM&gt;
     *      &lt;YDIM unit="M"&gt; pixelSizeY &lt;/YDIM&gt;
     *  &lt;/Geoposition_Insert&gt;
     *  &lt;Simplified_Location_Model&gt;
     *      &lt;Direct_Location_Model order="orderOfPolynom"&gt;
     *          &lt;lc_List&gt;
     *              &lt;lc index="i"&gt; lonCoefficients[i] &lt;/lc&gt;
     *          &lt;/lc_List&gt;
     *          &lt;pc_List&gt;
     *              &lt;pc index="i"&gt; latCoefficients[i] &lt;/pc&gt;
     *          &lt;/pc_List&gt;
     *      &lt;/Direct_Location_Model&gt;
     *      &lt;Reverse_Location_Model order="orderOfPolynom"&gt;
     *          &lt;ic_List&gt;
     *              &lt;ic index="i"&gt; iCoefficients[i] &lt;/ic&gt;
     *          &lt;/ic_List&gt;
     *          &lt;jc_List&gt;
     *              &lt;jc index="i"&gt; jCoefficients[i] &lt;/jc&gt;
     *          &lt;/jc_List&gt;
     *      &lt;/Reverse_Location_Model&gt;
     *  &lt;/Simplified_Location_Model&gt;
     * &lt;/Geoposition&gt;
     * </pre>
     * </td> </tr>
     * <tr> <td> </td>
     * <td> Since version 3.5 BEAM supports {@link org.esa.beam.framework.dataop.maptransf.MapInfo#setOrientation(float)}
     * Affected tag:<br>
     * &lt;Map_Info&gt; it is now written as follows:<br>
     * <pre>
     *  &lt;MAP_INFO&gt;
     *    &lt;PIXEL_X value="referencePixelX" /&gt;
     *    &lt;PIXEL_Y value="referencePixelY" /&gt;
     *    &lt;EASTING value="easting" /&gt;
     *    &lt;NORTHING value="northing" /&gt;
     *    &lt;ORIENTATION value="orientation" /&gt;
     *    &lt;PIXELSIZE_X value="pixelSizeX" /&gt;
     *    &lt;PIXELSIZE_Y value="pixelSizeY" /&gt;
     *    &lt;NODATA_VALUE value="noDataValue" /&gt;
     *    &lt;MAPUNIT value="mapUnit" /&gt;
     *    &lt;ORTHORECTIFIED value="orthorectified" /&gt;
     *    &lt;ELEVATION_MODEL value="elevModelName" /&gt;
     *    &lt;SCENE_FITTED value="sceneFitted" /&gt;
     *    &lt;SCENE_WIDTH value="sceneWidth" /&gt;
     *    &lt;SCENE_HEIGHT value="sceneHeight" /&gt;
     *    &lt;RESAMPLING value="resamplingName" /&gt;
     *  &lt;/MAP_INFO&gt;
     * </pre>
     * </td></tr>
     * <tr> <td valign="top">2.1.0</td>
     * <td> Since version 3.6 BEAM supports {@link org.esa.beam.framework.datamodel.GeneralFilterBand} and
     * {@link org.esa.beam.framework.datamodel.ConvolutionFilterBand}.<br>
     * Affected Tag:<br>
     * The tag Spectral_Band_Info can now have the following new tags
     * New tags:<br>
     * <pre>
     * <b>ConvolutionFilterBand</b>
     *  &lt;Filter_Band_Info bandType="ConvolutionFilterBand"&gt;
     *    &lt;FILTER_SOURCE&gt;bandName&lt;/FILTER_SOURCE&gt;
     *    &lt;Filter_Kernel&gt;
     *      &lt;KERNEL_WIDTH&gt;kernelWidth&lt;/KERNEL_WIDTH&gt;
     *      &lt;KERNEL_HEIGHT&gt;kernelHeight&lt;/KERNEL_HEIGHT&gt;
     *      &lt;KERNEL_FACTOR&gt;kernelFactor&lt;/KERNEL_FACTOR&gt;
     *      &lt;KERNEL_DATA&gt;kernelData[]&lt;/KERNEL_DATA&gt;
     *    &lt;/Filter_Kernel&gt;
     *  &lt;/Filter_Band_Info&gt;
     * <p/>
     * <b>GeneralFilterBand</b>
     * &lt;Filter_Band_Info bandType="GeneralFilterBand"&gt;
     *   &lt;FILTER_SOURCE&gt;bandName&lt;/FILTER_SOURCE&gt;
     *   &lt;FILTER_SUB_WINDOW_WIDTH&gt;subWindowWidth&lt;/FILTER_SUB_WINDOW_WIDTH&gt;
     *   &lt;FILTER_SUB_WINDOW_HEIGHT&gt;subWindowHeight&lt;/FILTER_SUB_WINDOW_HEIGHT&gt;
     *   &lt;FILTER_OPERATOR_CLASS_NAME&gt;operatorClassName&lt;/FILTER_OPERATOR_CLASS_NAME&gt;
     * &lt;/Filter_Band_Info&gt;
     * </pre>
     * </td></tr>
     * <tr>
     * <td valign="top">2.2.0</td>
     * <td>
     * After a bugfixing in the ProductData classes, the DIMAP Product Format<br>
     * have canged the output of uInt8, uInt16, uInt32 and UTC Values.
     * <ul>
     * <li>
     * Now the unsigned values are written as unsigned Values.<br>
     * Formally the large unsigned values was written as complementary<br>
     * negative values.
     * </li>
     * <li>
     * Now the UTG Value was written as human readable text.<br>
     * The UTC Value formally was written as a sequence of three values<br>
     * which defines the date, the seconds an the microseconds fraction.
     * </li>
     * </ul>
     * In this Version it ist possible to open older formats too.
     * </td>
     * </tr>
     * <tr> <td valign="top">2.3.0</td>
     * <td> Since version 4.1 BEAM supports ground control points (GCPs),
     * though the management of pins was redesgined. Pins and GCPs are stored in seperate groups.<br>
     * New tags:<br>
     * <pre>
     * <b>Pin_Group</b>
     *  &lt;Pin_Group&gt;
     *    &lt;Placemark&gt;
     *    &lt;/Placemark&gt;
     *  &lt;/Pin_Group&gt;
     * <b>Gcp_Group</b>
     *  &lt;Gcp_Group&gt;
     *    &lt;Placemark&gt;
     *    &lt;/Placemark&gt;
     *  &lt;/Gcp_Group&gt;
     * </pre>
     * </td></tr>
     * <tr> <td> </td>
     * <td> To harmonize the structur of the xml tree the Bitmask_Definition tag was moved to
     * to a new surrounding tag.
     * New tag:<br>
     * <pre>
     * <b>Bitmask_Definitions</b>
     *  &lt;Bitmask_Definitions&gt;
     *    &lt;Bitmask_Definition&gt;
     *    &lt;/Bitmask_Definition&gt;
     *  &lt;/Bitmask_Definitions&gt;
     * </pre>
     * </td></tr>
     * <tr>
     * <td valign="top">2.4.0</td>
     * <td> Since version 4.2 BEAM, we have
     * <pre>
     *  &lt;NO_DATA_COLOR red="234" green="25" blue="0" alpha="255" /&gt;
     *  &lt;HISTOGRAM_MATCHING&gt;Normalize&lt;/HISTOGRAM_MATCHING&gt;
     * </pre>
     * </td>
     * </tr>
     * </table>
     */
    public final static String DIMAP_CURRENT_VERSION = "2.4.0";

    // BEAM-Dimap default text
    public final static String DIMAP_METADATA_PROFILE = "BEAM-DATAMODEL-V1";
    public final static String DIMAP_DATASET_SERIES = "BEAM-PRODUCT";
    public final static String DATASET_PRODUCER_NAME = " ";
    //    public final static String DATASET_PRODUCER_NAME = "Brockmann-Consult | Phone +49 (04152) 889 300";
    public final static String DATA_FILE_FORMAT = "ENVI";
    public final static String DATA_FILE_FORMAT_DESCRIPTION = "ENVI File Format";
    public final static String DATA_FILE_ORGANISATION = "BAND_SEPARATE";

    // BEAM-Dimap document root tag
    public final static String TAG_ROOT = "Dimap_Document";

    // BEAM-Dimap metadata ID tags
    public final static String TAG_METADATA_ID = "Metadata_Id";
    public final static String TAG_METADATA_FORMAT = "METADATA_FORMAT";
    public final static String TAG_METADATA_PROFILE = "METADATA_PROFILE";

    // BEAM-Dimap production tags
    public final static String TAG_PRODUCTION = "Production";
    public final static String TAG_DATASET_PRODUCER_NAME = "DATASET_PRODUCER_NAME";
    public final static String TAG_DATASET_PRODUCER_URL = "DATASET_PRODUCER_URL";
    public final static String TAG_DATASET_PRODUCTION_DATE = "DATASET_PRODUCTION_DATE";
    public final static String TAG_JOB_ID = "JOB_ID";
    public final static String TAG_PRODUCT_TYPE = "PRODUCT_TYPE";
    public final static String TAG_PRODUCT_INFO = "PRODUCT_INFO";
    public final static String TAG_PROCESSING_REQUEST = "PROCESSING_REQUEST";
    public final static String TAG_REQUEST = "Request";
    public final static String TAG_PARAMETER = "Parameter";
    public final static String TAG_INPUTPRODUCT = "InputProduct";
    public final static String TAG_OUTPUTPRODUCT = "OutputProduct";
    public final static String TAG_PRODUCT_SCENE_RASTER_START_TIME = "PRODUCT_SCENE_RASTER_START_TIME";
    public final static String TAG_PRODUCT_SCENE_RASTER_STOP_TIME = "PRODUCT_SCENE_RASTER_STOP_TIME";
    public final static String TAG_OLD_SCENE_RASTER_START_TIME = "SENSING_START";
    public final static String TAG_OLD_SCENE_RASTER_STOP_TIME = "SENSING_STOP";

    // BEAM-Dimap geocoding tags
    public final static String TAG_COORDINATE_REFERENCE_SYSTEM = "Coordinate_Reference_System";
    public final static String TAG_GEOCODING_TIE_POINT_GRIDS = "Geocoding_Tie_Point_Grids";
    public final static String TAG_TIE_POINT_GRID_NAME_LAT = "TIE_POINT_GRID_NAME_LAT";
    public final static String TAG_TIE_POINT_GRID_NAME_LON = "TIE_POINT_GRID_NAME_LON";
    public final static String TAG_GEOCODING_MAP = "Geocoding_Map";
    public final static String TAG_GEOCODING_MAP_INFO = "MAP_INFO";

    //  -since version 2.0.0
    public final static String TAG_HORIZONTAL_CS_TYPE = "HORIZONTAL_CS_TYPE";


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

    public final static String TAG_GEOPOSITION = "Geoposition";
    public final static String TAG_GEOPOSITION_INSERT = "Geoposition_Insert";
    public final static String TAG_ULX_MAP = "ULXMAP";
    public final static String TAG_ULY_MAP = "ULYMAP";
    public final static String TAG_X_DIM = "XDIM";
    public final static String TAG_Y_DIM = "YDIM";
    public final static String TAG_SIMPLIFIED_LOCATION_MODEL = "Simplified_Location_Model";
    public final static String TAG_DIRECT_LOCATION_MODEL = "Direct_Location_Model";
    public final static String TAG_LC_LIST = "lc_List";
    public final static String TAG_LC = "lc";
    public final static String TAG_PC_LIST = "pc_List";
    public final static String TAG_PC = "pc";
    public final static String TAG_REVERSE_LOCATION_MODEL = "Reverse_Location_Model";
    public final static String TAG_IC_LIST = "ic_List";
    public final static String TAG_IC = "ic";
    public final static String TAG_JC_LIST = "jc_List";
    public final static String TAG_JC = "jc";

    //   - since version 1.4.0
    public final static String TAG_GEO_TABLES = "GEO_TABLES";
    public final static String TAG_HORIZONTAL_CS = "Horizontal_CS";
    public final static String TAG_HORIZONTAL_CS_NAME = "HORIZONTAL_CS_NAME";
    public final static String TAG_GEOGRAPHIC_CS = "Geographic_CS";
    public final static String TAG_GEOGRAPHIC_CS_NAME = "GEOGRAPHIC_CS_NAME";
    public final static String TAG_HORIZONTAL_DATUM = "Horizontal_Datum";
    public final static String TAG_HORIZONTAL_DATUM_NAME = "HORIZONTAL_DATUM_NAME";
    public final static String TAG_ELLIPSOID = "Ellipsoid";
    public final static String TAG_ELLIPSOID_NAME = "ELLIPSOID_NAME";
    public final static String TAG_ELLIPSOID_PARAMETERS = "Ellipsoid_Parameters";
    public final static String TAG_ELLIPSOID_MAJ_AXIS = "ELLIPSOID_MAJ_AXIS";
    public final static String TAG_ELLIPSOID_MIN_AXIS = "ELLIPSOID_MIN_AXIS";
    public final static String TAG_PROJECTION = "Projection";
    public final static String TAG_PROJECTION_NAME = "NAME";
    public final static String TAG_PROJECTION_CT_METHOD = "Projection_CT_Method";
    public final static String TAG_PROJECTION_CT_NAME = "PROJECTION_CT_NAME";
    public final static String TAG_PROJECTION_PARAMETERS = "Projection_Parameters";
    public final static String TAG_PROJECTION_PARAMETER = "Projection_Parameter";
    public final static String TAG_PROJECTION_PARAMETER_NAME = "PROJECTION_PARAMETER_NAME";
    public final static String TAG_PROJECTION_PARAMETER_VALUE = "PROJECTION_PARAMETER_VALUE";

    // BEAM-Dimap dataset id tags
    public final static String TAG_DATASET_ID = "Dataset_Id";
    public final static String TAG_DATASET_INDEX = "DATASET_INDEX";
    public final static String TAG_DATASET_SERIES = "DATASET_SERIES";
    public final static String TAG_DATASET_NAME = "DATASET_NAME";
    public final static String TAG_DATASET_DESCRIPTION = "DATASET_DESCRIPTION";
    public final static String TAG_COPYRIGHT = "COPYRIGHT";
    public final static String TAG_COUNTRY_NAME = "COUNTRY_NAME";
    public final static String TAG_COUNTRY_CODE = "COUNTRY_CODE";
    public final static String TAG_DATASET_LOCATION = "DATASET_LOCATION";
    public final static String TAG_DATASET_TN_PATH = "DATASET_TN_PATH";
    public final static String TAG_DATASET_TN_FORMAT = "DATASET_TN_FORMAT";
    public final static String TAG_DATASET_QL_PATH = "DATASET_QL_PATH";
    public final static String TAG_DATASET_QL_FORMAT = "DATASET_QL_FORMAT";

    // BEAM_Dimap dataset use tags
    public final static String TAG_DATASET_USE = "Dataset_Use";
    public final static String TAG_DATASET_COMMENTS = "DATASET_COMMENTS";

    // BEAM-Dimap flag coding tags
    // todo - FIXME!!! this is against DIMAP naming convention!!! (nf)
    public final static String TAG_FLAG_CODING = "Flag_Coding";
    public final static String TAG_FLAG = "Flag";
    public final static String TAG_FLAG_NAME = "Flag_Name";
    public final static String TAG_FLAG_INDEX = "Flag_Index";
    public final static String TAG_FLAG_DESCRIPTION = "Flag_description";

    // BEAM-Dimap index coding tags
    public final static String TAG_INDEX_CODING = "Index_Coding";
    public final static String TAG_INDEX = "Index";
    public final static String TAG_INDEX_NAME = "INDEX_NAME";
    public final static String TAG_INDEX_VALUE = "INDEX_VALUE";
    public final static String TAG_INDEX_DESCRIPTION = "INDEX_DESCRIPTION";

    // BEAM-Dimap raster dimension tags
    public final static String TAG_RASTER_DIMENSIONS = "Raster_Dimensions";
    public final static String TAG_NCOLS = "NCOLS";
    public final static String TAG_NROWS = "NROWS";
    public final static String TAG_NBANDS = "NBANDS";

    // BEAM-Dimap tie point grid tags
    public final static String TAG_TIE_POINT_GRIDS = "Tie_Point_Grids";
    public final static String TAG_TIE_POINT_NUM_TIE_POINT_GRIDS = "NUM_TIE_POINT_GRIDS";
    public final static String TAG_TIE_POINT_GRID_INFO = "Tie_Point_Grid_Info";
    public final static String TAG_TIE_POINT_GRID_INDEX = "TIE_POINT_GRID_INDEX";
    public final static String TAG_TIE_POINT_DESCRIPTION = "TIE_POINT_DESCRIPTION";
    public final static String TAG_TIE_POINT_PHYSICAL_UNIT = "PHYSICAL_UNIT";
    public final static String TAG_TIE_POINT_GRID_NAME = "TIE_POINT_GRID_NAME";
    public final static String TAG_TIE_POINT_DATA_TYPE = "DATA_TYPE";
    public final static String TAG_TIE_POINT_NCOLS = "NCOLS";
    public final static String TAG_TIE_POINT_NROWS = "NROWS";
    public final static String TAG_TIE_POINT_OFFSET_X = "OFFSET_X";
    public final static String TAG_TIE_POINT_OFFSET_Y = "OFFSET_Y";
    public final static String TAG_TIE_POINT_STEP_X = "STEP_X";
    public final static String TAG_TIE_POINT_STEP_Y = "STEP_Y";
    public final static String TAG_TIE_POINT_CYCLIC = "CYCLIC";

    // BEAM-Dimap data access tags
    public final static String TAG_DATA_ACCESS = "Data_Access";
    public final static String TAG_DATA_FILE_FORMAT = "DATA_FILE_FORMAT";
    public final static String TAG_DATA_FILE_FORMAT_DESC = "DATA_FILE_FORMAT_DESC";
    public final static String TAG_DATA_FILE_ORGANISATION = "DATA_FILE_ORGANISATION";
    public final static String TAG_DATA_FILE = "Data_File";
    public final static String TAG_DATA_FILE_PATH = "DATA_FILE_PATH";
    public final static String TAG_BAND_INDEX = "BAND_INDEX";
    public final static String TAG_TIE_POINT_GRID_FILE = "Tie_Point_Grid_File";
    public final static String TAG_TIE_POINT_GRID_FILE_PATH = "TIE_POINT_GRID_FILE_PATH";

    // BEAM-Dimap image display tags
    public final static String TAG_IMAGE_DISPLAY = "Image_Display";
    public final static String TAG_BAND_STATISTICS = "Band_Statistics";
    public final static String TAG_STX_MIN = "STX_MIN";
    public final static String TAG_STX_MAX = "STX_MAX";
    public final static String TAG_STX_MEAN = "STX_MEAN";
    public final static String TAG_STX_STDV = "STX_STDV";
    public final static String TAG_STX_LIN_MIN = "STX_LIN_MIN";
    public final static String TAG_STX_LIN_MAX = "STX_LIN_MAX";
    public final static String TAG_HISTOGRAM = "HISTOGRAM";
    public final static String TAG_NUM_COLORS = "NUM_COLORS";
    public final static String TAG_COLOR_PALETTE_POINT = "Color_Palette_Point";
    public final static String TAG_SAMPLE = "SAMPLE";
    public final static String TAG_LABEL = "LABEL";
    public final static String TAG_COLOR = "COLOR";
    public final static String TAG_GAMMA = "GAMMA";
    public final static String TAG_NO_DATA_COLOR = "NO_DATA_COLOR";
    public final static String TAG_HISTOGRAM_MATCHING = "HISTOGRAM_MATCHING";
    public final static String TAG_BITMASK_OVERLAY = "Bitmask_Overlay";
    public final static String TAG_BITMASK = "BITMASK";
    public final static String TAG_ROI_DEFINITION = "ROI_Definition";
    public final static String TAG_ROI_ONE_DIMENSIONS = "ROI_ONE_DIMENSIONS";
    public final static String TAG_VALUE_RANGE_MAX = "VALUE_RANGE_MAX";
    public final static String TAG_VALUE_RANGE_MIN = "VALUE_RANGE_MIN";
    public final static String TAG_BITMASK_ENABLED = "BITMASK_ENABLED";
    public final static String TAG_INVERTED = "INVERTED";
    public final static String TAG_OR_COMBINED = "OR_COMBINED";
    public final static String TAG_SHAPE_ENABLED = "SHAPE_ENABLED";
    public final static String TAG_SHAPE_FIGURE = "Shape_Figure";
    public final static String TAG_VALUE_RANGE_ENABLED = "VALUE_RANGE_ENABLED";
    public final static String TAG_PATH_SEG = "SEGMENT";
    public final static String TAG_PIN_USE_ENABLED = "PIN_USE_ENABLED";

    // BEAM-Dimap image interpretation tags
    public final static String TAG_IMAGE_INTERPRETATION = "Image_Interpretation";
    public final static String TAG_SPECTRAL_BAND_INFO = "Spectral_Band_Info";
    public final static String TAG_VIRTUAL_BAND_INFO = "Virtual_Band_Info";
    public final static String TAG_BAND_DESCRIPTION = "BAND_DESCRIPTION";
    public final static String TAG_PHYSICAL_GAIN = "PHYSICAL_GAIN";
    public final static String TAG_PHYSICAL_BIAS = "PHYSICAL_BIAS";
    public final static String TAG_PHYSICAL_UNIT = "PHYSICAL_UNIT";
    public final static String TAG_BAND_NAME = "BAND_NAME";
    public final static String TAG_DATA_TYPE = "DATA_TYPE";
    public final static String TAG_SOLAR_FLUX = "SOLAR_FLUX";
    public final static String TAG_SPECTRAL_BAND_INDEX = "SPECTRAL_BAND_INDEX";
    public final static String TAG_SOLAR_FLUX_UNIT = "SOLAR_FLUX_UNIT";
    public final static String TAG_BANDWIDTH = "BANDWIDTH";
    public final static String TAG_BAND_WAVELEN = "BAND_WAVELEN";
    public final static String TAG_WAVELEN_UNIT = "WAVELEN_UNIT";
    public final static String TAG_FLAG_CODING_NAME = "FLAG_CODING_NAME";
    public final static String TAG_INDEX_CODING_NAME = "INDEX_CODING_NAME";
    public final static String TAG_SCALING_FACTOR = "SCALING_FACTOR";
    public final static String TAG_SCALING_OFFSET = "SCALING_OFFSET";
    public final static String TAG_SCALING_LOG_10 = "LOG10_SCALED";
    public final static String TAG_VALID_MASK_TERM = "VALID_MASK_TERM";
    public final static String TAG_NO_DATA_VALUE_USED = "NO_DATA_VALUE_USED";
    public final static String TAG_NO_DATA_VALUE = "NO_DATA_VALUE";

    //Virtual bands support
    public final static String TAG_VIRTUAL_BAND = "VIRTUAL_BAND";
    public final static String TAG_VIRTUAL_BAND_CHECK_INVALIDS = "CHECK_INVALIDS";
    public final static String TAG_VIRTUAL_BAND_EXPRESSION = "EXPRESSION";
    public final static String TAG_VIRTUAL_BAND_INVALID_VALUE = "INVALID_VALUE";
    public final static String TAG_VIRTUAL_BAND_USE_INVALID_VALUE = "USE_INVALID_VALUE";

    // Filter bands support
    public static final String TAG_FILTER_BAND_INFO = "Filter_Band_Info";
    public static final String TAG_FILTER_SOURCE = "FILTER_SOURCE";
    public static final String TAG_FILTER_KERNEL = "Filter_Kernel";
    public static final String TAG_FILTER_SUB_WINDOW_WIDTH = "FILTER_SUB_WINDOW_WIDTH";
    public static final String TAG_FILTER_SUB_WINDOW_HEIGHT = "FILTER_SUB_WINDOW_HEIGHT";
    public static final String TAG_FILTER_OPERATOR_CLASS_NAME = "FILTER_OPERATOR_CLASS_NAME";

    // Kernel support
    public static final String TAG_KERNEL_HEIGHT = "KERNEL_HEIGHT";
    public static final String TAG_KERNEL_WIDTH = "KERNEL_WIDTH";
    public static final String TAG_KERNEL_FACTOR = "KERNEL_FACTOR";
    public static final String TAG_KERNEL_DATA = "KERNEL_DATA";

    // BEAM-Dimap dataset sources tags
    public final static String TAG_DATASET_SOURCES = "Dataset_Sources";
    public final static String TAG_SOURCE_INFORMATION = "Source_Information";
    public final static String TAG_SOURCE_ID = "SOURCE_ID";
    public final static String TAG_SOURCE_TYPE = "SOURCE_TYPE";
    public final static String TAG_SOURCE_DESCRIPTION = "SOURCE_DESCRIPTION";
    public final static String TAG_SOURCE_FRAME = "Source_Frame";
    public final static String TAG_VERTEX = "Vertex";
    public final static String TAG_FRAME_LON = "FRAME_LON";
    public final static String TAG_FRAME_LAT = "FRAME_LAT";
    public final static String TAG_FRAME_X = "FRAME_X";
    public final static String TAG_FRAME_Y = "FRAME_Y";
    public final static String TAG_SCENE_SOURCE = "Scene_Source";
    public final static String TAG_MISSION = "MISSION";
    public final static String TAG_INSTRUMENT = "INSTRUMENT";
    public final static String TAG_IMAGING_MODE = "IMAGING_MODE";
    public final static String TAG_IMAGING_DATE = "IMAGING_DATE";
    public final static String TAG_IMAGING_TIME = "IMAGING_TIME";
    public final static String TAG_GRID_REFERENCE = "GRID_REFERENCE";
    public final static String TAG_SCENE_RECTIFICATION_ELEV = "SCENE_RECTIFICATION_ELEV";
    public final static String TAG_INCIDENCE_ANGLE = "INCIDENCE_ANGLE";
    public final static String TAG_THEORETICAL_RESOLUTION = "THEORETICAL_RESOLUTION";
    public final static String TAG_SUN_AZIMUTH = "SUN_AZIMUTH";
    public final static String TAG_SUN_ELEVATION = "SUN_ELEVATION";
    public final static String TAG_METADATA_ELEMENT = "MDElem";
    public final static String TAG_METADATA_VALUE = "VALUE";
    public final static String TAG_METADATA_ATTRIBUTE = "MDATTR";

    // BEAM-Dimap bitmask definition tags
    public final static String TAG_BITMASK_DEFINITIONS = "Bitmask_Definitions";
    public final static String TAG_BITMASK_DEFINITION = "Bitmask_Definition";
    public final static String TAG_BITMASK_DESCRIPTION = "DESCRIPTION";
    public final static String TAG_BITMASK_EXPRESSION = "EXPRESSION";
    public final static String TAG_BITMASK_COLOR = TAG_COLOR;
    public final static String TAG_BITMASK_TRANSPARENCY = "TRANSPARENCY";

    // BEAM-Dimap placemark tags
    public final static String TAG_PLACEMARK = "Placemark";
    public final static String TAG_PLACEMARK_LABEL = "LABEL";
    public final static String TAG_PLACEMARK_DESCRIPTION = "DESCRIPTION";
    public final static String TAG_PLACEMARK_LATITUDE = "LATITUDE";
    public final static String TAG_PLACEMARK_LONGITUDE = "LONGITUDE";
    public final static String TAG_PLACEMARK_PIXEL_X = "PIXEL_X";
    public final static String TAG_PLACEMARK_PIXEL_Y = "PIXEL_Y";
    public final static String TAG_PLACEMARK_FILL_COLOR = "FillColor";
    public final static String TAG_PLACEMARK_OUTLINE_COLOR = "OutlineColor";

    // BEAM-Dimap pin tags
    public final static String TAG_PIN_GROUP = "Pin_Group";
    public final static String TAG_PIN = "Pin";

    // BEAM-Dimap gcp tags
    public final static String TAG_GCP_GROUP = "Gcp_Group";

    /**
     * @deprecated in 4.1, use {@link #TAG_PLACEMARK_LABEL}
     */
    @Deprecated
    public final static String TAG_PIN_LABEL = TAG_PLACEMARK_LABEL;
    /**
     * @deprecated in 4.1, use {@link #TAG_PLACEMARK_DESCRIPTION}
     */
    @Deprecated
    public final static String TAG_PIN_DESCRIPTION;

    static {
        TAG_PIN_DESCRIPTION = TAG_PLACEMARK_DESCRIPTION;
    }

    /**
     * @deprecated in 4.1, use {@link #TAG_PLACEMARK_LATITUDE}
     */
    @Deprecated
    public final static String TAG_PIN_LATITUDE = TAG_PLACEMARK_LATITUDE;
    /**
     * @deprecated in 4.1, use {@link #TAG_PLACEMARK_LONGITUDE}
     */
    @Deprecated
    public final static String TAG_PIN_LONGITUDE = TAG_PLACEMARK_LONGITUDE;
    /**
     * @deprecated in 4.1, use {@link #TAG_PLACEMARK_PIXEL_X}
     */
    @Deprecated
    public final static String TAG_PIN_PIXEL_X = TAG_PLACEMARK_PIXEL_X;
    /**
     * @deprecated in 4.1, use {@link #TAG_PLACEMARK_PIXEL_Y}
     */
    @Deprecated
    public final static String TAG_PIN_PIXEL_Y = TAG_PLACEMARK_PIXEL_Y;
    /**
     * @deprecated in 4.1, use {@link #TAG_PLACEMARK_FILL_COLOR}
     */
    @Deprecated
    public final static String TAG_PIN_FILL_COLOR = TAG_PLACEMARK_FILL_COLOR;
    /**
     * @deprecated in 4.1, use {@link #TAG_PLACEMARK_OUTLINE_COLOR}
     */
    @Deprecated
    public final static String TAG_PIN_OUTLINE_COLOR = TAG_PLACEMARK_OUTLINE_COLOR;

    // attribute
    public final static String ATTRIB_RED = "red";
    public final static String ATTRIB_GREEN = "green";
    public final static String ATTRIB_BLUE = "blue";
    public final static String ATTRIB_ALPHA = "alpha";
    public final static String ATTRIB_NAMES = "names";
    public final static String ATTRIB_DESCRIPTION = "desc";
    public final static String ATTRIB_UNIT = "unit";
    public final static String ATTRIB_MODE = "mode";
    public final static String ATTRIB_TYPE = "type";
    public final static String ATTRIB_ELEMS = "elems";
    public final static String ATTRIB_NAME = "name";
    public final static String ATTRIB_VERSION = "version";
    public final static String ATTRIB_HREF = "href";
    public final static String ATTRIB_VALUE = "value";
    public final static String ATTRIB_ORDER = "order";
    public final static String ATTRIB_INDEX = "index";
    public static final String ATTRIB_BAND_TYPE = "bandType";

}
