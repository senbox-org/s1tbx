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

import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.FXYGeoCoding;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The <code>DimapProductWriterPlugIn</code> class is the plug-in entry-point for the BEAM-DIMAP product writer.
 * <p>
 * <p>
 * <b>Version history:</b>
 * <p>
 * <p>
 * <table summary="">
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
 * &lt;CYCLIC&gt;<br> </td> </tr>
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
 * &lt;GAMMA&gt;<br><br>
 * Now supports <code>ValidMasks</code> for bands.<br>
 * affected tag:<br>
 * &lt;Spectral_Band_Info&gt;
 * inside &lt;Image_Interpretation&gt;<br>
 * new tags added:<br>
 * &lt;VALID_MASK_TERM&gt;<br> </td> </tr>
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
 * &lt;PRODUCT_SCENE_RASTER_START_TIME&gt; &lt;PRODUCT_SCENE_RASTER_STOP_TIME&gt;<br> </td> </tr>
 * <tr> <td valign="top">1.4.2</td>
 * <td> Since BEAM version 3.2 {@link RasterDataNode RasterDataNode} supports the
 * <code>NoDataValue</code>.<br>
 * Affected tag:<br>
 * &lt;Spectral_Band_Info&gt; inside of &lt;Image_Interpretation&gt;<br>
 * Following tags are replaced:<br>
 * &lt;INVALID_VALUE&gt; replaced by &lt;NO_DATA_VALUE&gt;<br>
 * &lt;USE_INVALID_VALUE&gt; replaced by &lt;NO_DATA_VALUE_USED&gt;<br> </td> </tr>
 * <tr> <td valign="top">2.0.0</td>
 * <td> Since version 3.5 BEAM supports {@link FXYGeoCoding}.<br>
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
 * <td> Since version 3.5 BEAM supports {@link MapInfo#setOrientation(float)}
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
 * <td> Since version 3.6 BEAM supports {@link GeneralFilterBand} and
 * {@link ConvolutionFilterBand}.<br>
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
 * <td> To harmonize the structure of the xml tree the Bitmask_Definition tag was moved to
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
 * <td> Since version BEAM 4.2, we have
 * <pre>
 *  &lt;NO_DATA_COLOR red="234" green="25" blue="0" alpha="255" /&gt;
 *  &lt;HISTOGRAM_MATCHING&gt;Normalize&lt;/HISTOGRAM_MATCHING&gt;
 * </pre>
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">2.5.0</td>
 * <td> Since BEAM 4.5, the width and height tags within the GeneralFilterBand element are replaced by a size tag:
 * <pre>
 * <b>These two tags</b>
 *   &lt;FILTER_SUB_WINDOW_WIDTH&gt;subWindowWidth&lt;/FILTER_SUB_WINDOW_WIDTH&gt;
 *   &lt;FILTER_SUB_WINDOW_HEIGHT&gt;subWindowHeight&lt;/FILTER_SUB_WINDOW_HEIGHT&gt;
 * <b>are replaced by</b>
 *   &lt;FILTER_SUB_WINDOW_SIZE&gt;subWindowSize&lt;/FILTER_SUB_WINDOW_SIZE&gt;
 * </pre>
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">2.6.0</td>
 * <td> Since BEAM 4.5.1 the band statistics tag is extended to beside the mininum and maximum value also the
 * mean value and the standard deviation:
 * <pre>
 * <b>These two tags are </b>
 *   &lt;STX_MEAN&gt;meanValue&lt;/STX_MEAN&gt;
 *   &lt;STX_STD_DEV&gt;stdDevValue&lt;/STX_STD_DEV&gt;
 * </pre>
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">2.6.1</td>
 * <td> No longer supporting
 * <pre>
 *   &lt;CHECK_INVALIDS&gt;true&lt;/CHECK_INVALIDS&gt;
 * </pre>
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">2.7.0</td>
 * <td> Since version BEAM 4.6.2 BEAM-DIMAP supports PixelGeoCoding.
 * <pre>
 *    &lt;Geoposition&gt;
 *        &lt;LATITUDE_BAND&gt; latBandName &lt;/LATITUDE_BAND&gt;
 *        &lt;LONGITUDE_BAND&gt; lonBandName &lt;/LONGITUDE_BAND&gt;
 *        &lt;VALID_MASK_EXPRESSION&gt; validMask &lt;/VALID_MASK_EXPRESSION&gt;
 *        &lt;SEARCH_RADIUS&gt; searchRadius &lt;/SEARCH_RADIUS&gt;
 *        &lt;Pixel_Position_Estimator&gt;
 *            &lt;!-- GeoCoding of the pixel position estimator --&gt;
 *        &lt;/Pixel_Position_Estimator&gt;
 *    &lt;/Geoposition&gt;
 * </pre>
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">2.8.0</td>
 * <td> Since version BEAM 4.7 BEAM-DIMAP supports CrsGeoCoding.
 * <pre>
 *    &lt;Coordinate_Reference_System&gt;
 *        &lt;WKT&gt;
 *            wktString
 *        &lt;/WKT&gt;
 *    &lt;/Coordinate_Reference_System&gt;
 *    &lt;Geoposition&gt;
 *        &lt;IMAGE_TO_MODEL_TRANSFORM&gt;matrix values&lt;/IMAGE_TO_MODEL_TRANSFORM&gt;
 *    &lt;/Geoposition&gt;
 * </pre>
 * In addition, the persistence of {@code BitmaskDef}s is obsolete, because {@code BitmaskDef}s
 * have been marked as deprecated and replaced with {@code Mask}s.
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">2.9.0</td>
 * <td> For BEAM 4.8, the element BAND_GROUPING has been introduced.
 * <pre>
 *    &lt;Image_Interpretation&gt;
 *        &lt;BAND_GROUPING&gt;pattern-1:pattern-2:pattern-3&lt;/BAND_GROUPING&gt;
 *        ...
 *    &lt;/Image_Interpretation&gt;
 * </pre>
 * In addition, the persistence of {@code BitmaskDef}s is obsolete, because {@code BitmaskDef}s
 * have been marked as deprecated and replaced with {@code Mask}s.
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">2.10.0</td>
 * <td> For BEAM 4.9, the element Geoposition_Points has been introduced.
 * <pre>
 *    &lt;Geoposition_Points&gt;
 *        &lt;INTERPOLATION_METHOD&gt;POLYNOMIAL[1|2|3]&lt;/INTERPOLATION_METHOD&gt;
 *        &lt;Original_Geocoding&gt;
 *            ...
 *        &lt;/Original_Geocoding&gt;
 *    &lt;/Geoposition_Points&gt;
 * </pre>
 * In addition, the persistence of the {@code TiePointGeoCoding} is now written into the {@code Geoposition}
 * element.
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">2.11.0</td>
 * <td> Since BEAM 4.10, Pins and Ground Control Points are written into the "vector_data" directory.
 * The XML elements "Pin_Group" and "GCP_Group" with their "Placemark" children are not used anymore.
 * </td>
 * </tr>
 * <tr>
 * <td valign="top"></td>
 * <td> Since BEAM 4.10, the XML elements "ROI", "ROI_Definition", and "ROI_ONE_DIMENSIONS" are not used anymore.
 * </td>
 * </tr>
 * <tr>
 * <td valign="top"></td>
 * <td> Since SNAP 2.0, crs geocodings can be written per band.
 * </td>
 * </tr>
 * </table>
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see DimapProductWriter
 */
public class DimapProductWriterPlugIn implements ProductWriterPlugIn {

    public final static String DIMAP_FORMAT_NAME = DimapProductConstants.DIMAP_FORMAT_NAME;
    private final SnapFileFilter dimapFileFilter = (SnapFileFilter) DimapProductHelpers.createDimapFileFilter();
    private Set<DimapProductWriter.WriterExtender> dimapWriterWriterExtenders;

    /**
     * Constructs a new BEAM-DIMAP product writer plug-in instance.
     */
    public DimapProductWriterPlugIn() {
    }

    @Override
    public EncodeQualification getEncodeQualification(Product product) {
        return EncodeQualification.FULL;
    }

    /**
     * Returns a string array containing the single entry <code>&quot;BEAM-DIMAP&quot;</code>.
     */
    public String[] getFormatNames() {
        return new String[]{DIMAP_FORMAT_NAME};
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return new String[]{DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION};
    }

    /**
     * Returns an array containing the classes that represent valid output types for this BEAM-DIMAP product writer.
     * <p> Intances of the classes returned in this array are valid objects for the <code>writeProductNodes</code>
     * method of the <code>AbstractProductWriter</code> interface (the method will not throw an
     * <code>InvalidArgumentException</code> in this case).
     *
     * @return an array containing valid output types, never <code>null</code>
     * @see AbstractProductWriter#writeProductNodes
     */
    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the locale name for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return "BEAM-DIMAP product writer";
    }

    /**
     * Creates an instance of the actual BEAM-DIMAP product writer class.
     *
     * @return a new instance of the <code>DimapProductWriter</code> class
     */
    public ProductWriter createWriterInstance() {
        final DimapProductWriter dimapProductWriter = new DimapProductWriter(this);
        if (dimapWriterWriterExtenders != null) {
            for (DimapProductWriter.WriterExtender writerExtender : dimapWriterWriterExtenders) {
                dimapProductWriter.addExtender(writerExtender);
            }
        }
        return dimapProductWriter;
    }

    public SnapFileFilter getProductFileFilter() {
        return dimapFileFilter;
    }

    public void addWriterExtender(DimapProductWriter.WriterExtender writerExtender) {
        if (dimapWriterWriterExtenders == null) {
            dimapWriterWriterExtenders = new HashSet<DimapProductWriter.WriterExtender>();
        }
        if (writerExtender != null) {
            dimapWriterWriterExtenders.add(writerExtender);
        }
    }

    public void removeWriterExtender(DimapProductWriter.WriterExtender writerExtender) {
        if (dimapWriterWriterExtenders == null) {
            dimapWriterWriterExtenders.remove(writerExtender);
        }
    }
}
