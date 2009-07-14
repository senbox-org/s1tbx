/*
 * $Id: DimapProductHelpers.java,v 1.7 2007/02/19 14:02:58 marcoz Exp $
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

import org.esa.beam.dataio.dimap.spi.DimapPersistable;
import org.esa.beam.dataio.dimap.spi.DimapPersistence;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.FXYGeoCoding;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PointingFactory;
import org.esa.beam.framework.datamodel.PointingFactoryRegistry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ROIDefinition;
import org.esa.beam.framework.datamodel.SampleCoding;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.XmlWriter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.util.math.FXYSum;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class defines some static methods used to create and access BEAM DIMAP XML documents.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class DimapProductHelpers {

    /**
     * Creates a in-memory data product represenation from the given DOM (BEAM-DIMAP format).
     *
     * @param dom the DOM in BEAM-DIMAP format
     *
     * @return an in-memory data product represenation
     */
    public static Product createProduct(Document dom) {
        return new ProductBuilder(dom).createProduct();
    }

    /**
     * Extract a <code>String</code> object from the given dom which points to the data for the band with the given
     * name.
     *
     * @param dom      the JDOM in BEAM-DIMAP format
     * @param product  the product to retrieve the data files for
     * @param inputDir the directory where to search for the data files
     *
     * @return a <code>Map</code> object which contains all the band data references from the given jDom.<br> Returns
     *         an empty <code>Map</code> if the jDom does not contain band data files
     *
     * @throws IllegalArgumentException if one of the parameters is null.
     */
    public static Map<Band, File> getBandDataFiles(final Document dom, final Product product,
                                                   final File inputDir) throws IllegalArgumentException {
        Guardian.assertNotNull("dom", dom);
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("inputDir", inputDir);

        final Map<Band, File> dataFilesMap = new HashMap<Band, File>();
        if (!dom.hasRootElement()) {
            return dataFilesMap;
        }
        final Element rootElement = dom.getRootElement();
        if (rootElement == null) {
            return dataFilesMap;
        }
        final Element dataAccess = rootElement.getChild(DimapProductConstants.TAG_DATA_ACCESS);
        if (dataAccess == null) {
            return dataFilesMap;
        }
        final List bandDataFiles = dataAccess.getChildren(DimapProductConstants.TAG_DATA_FILE);
        for (Object bandDataFile1 : bandDataFiles) {
            final Element bandDataFile = (Element) bandDataFile1;
            final String actualIndex = bandDataFile.getChildTextTrim(DimapProductConstants.TAG_BAND_INDEX);
            final String bandName = getBandName(rootElement, actualIndex);
            final Band band = product.getBand(bandName);
            if (band != null) {
                final Element filePathElement = bandDataFile.getChild(DimapProductConstants.TAG_DATA_FILE_PATH);
                final String bandHeaderFilePath = filePathElement.getAttributeValue(DimapProductConstants.ATTRIB_HREF);
                if (bandHeaderFilePath != null && bandHeaderFilePath.length() > 0) {
                    final String localHeaderFilePath = SystemUtils.convertToLocalPath(bandHeaderFilePath);
                    final String bandDataFilePath = FileUtils.exchangeExtension(localHeaderFilePath,
                                                                                DimapProductConstants.IMAGE_FILE_EXTENSION);
                    dataFilesMap.put(band, new File(inputDir, bandDataFilePath));
                }
            }
        }

        return dataFilesMap;
    }

    /**
     * Extract a <code>String</code> object from the given dom which points to the data for the tie point grid with the
     * given name.
     *
     * @param dom              the DOM in BEAM-DIMAP format
     * @param tiePointGridName the name of the tie point grid
     *
     * @return the <code>String</code> object which points to the data for the tie point grid.
     *
     * @throws IllegalArgumentException if the parameter dom is null or the parameter tiePointGridName is null or empty
     */
    public static String getTiePointDataFile(Document dom, String tiePointGridName) throws IllegalArgumentException {
        Guardian.assertNotNull("dom", dom);
        Guardian.assertNotNullOrEmpty("tiePointGridName", tiePointGridName);
        final Element rootElement = dom.getRootElement();
        Element dataAccess = rootElement.getChild(DimapProductConstants.TAG_DATA_ACCESS);
        if (dataAccess == null) {
            return null;
        }
        final String index = getTiePointGridIndex(rootElement, tiePointGridName);
        if (index == null) {
            return null;
        }
        List tiePointGridFiles = dataAccess.getChildren(DimapProductConstants.TAG_TIE_POINT_GRID_FILE);
        for (Object child : tiePointGridFiles) {
            final Element tiePointGridFile = (Element) child;
            final String actualIndex = tiePointGridFile.getChildTextTrim(
                    DimapProductConstants.TAG_TIE_POINT_GRID_INDEX);
            if (index.equals(actualIndex)) {
                final Element filePathElement = tiePointGridFile.getChild(
                        DimapProductConstants.TAG_TIE_POINT_GRID_FILE_PATH);
                final String tiePointGridFilePath = filePathElement.getAttributeValue(
                        DimapProductConstants.ATTRIB_HREF);
                if (tiePointGridFilePath != null && tiePointGridFilePath.length() > 0) {
                    return SystemUtils.convertToLocalPath(tiePointGridFilePath);
                }
            }
        }
        return null;
    }

    public static void printColorTag(int indent, Color color, XmlWriter pw) {
        printColorTag(indent, DimapProductConstants.TAG_COLOR, color, pw);
    }

    public static void printColorTag(int indent, String tag, Color color, XmlWriter pw) {
        if (color == null) {
            return;
        }
        if (pw == null) {
            return;
        }
        final String[][] attributes = new String[4][];
        attributes[0] = new String[]{DimapProductConstants.ATTRIB_RED, String.valueOf(color.getRed())};
        attributes[1] = new String[]{DimapProductConstants.ATTRIB_GREEN, String.valueOf(color.getGreen())};
        attributes[2] = new String[]{DimapProductConstants.ATTRIB_BLUE, String.valueOf(color.getBlue())};
        attributes[3] = new String[]{DimapProductConstants.ATTRIB_ALPHA, String.valueOf(color.getAlpha())};
        pw.printLine(indent, tag, attributes, null);
    }

    public static Color createColor(Element colorElem) {
        int red = Integer.parseInt(colorElem.getAttributeValue(DimapProductConstants.ATTRIB_RED));
        int green = Integer.parseInt(colorElem.getAttributeValue(DimapProductConstants.ATTRIB_GREEN));
        int blue = Integer.parseInt(colorElem.getAttributeValue(DimapProductConstants.ATTRIB_BLUE));
        final String alphaStr = colorElem.getAttributeValue(DimapProductConstants.ATTRIB_ALPHA);
        int alpha = alphaStr != null ? Integer.parseInt(alphaStr) : 255;
        return new Color(red, green, blue, alpha);
    }


    static GeoCoding[] createGeoCoding(Document dom, Product product) {
        Debug.assertNotNull(dom);
        Debug.assertNotNull(product);
        if (dom.getRootElement().getChild(DimapProductConstants.TAG_GEOPOSITION) != null) {

            final Element crsElem = dom.getRootElement().getChild(
                    DimapProductConstants.TAG_COORDINATE_REFERENCE_SYSTEM);
            final Element hcsElem = crsElem.getChild(DimapProductConstants.TAG_HORIZONTAL_CS);
            final Element gcsElem = hcsElem.getChild(DimapProductConstants.TAG_GEOGRAPHIC_CS);
            final Element horizDatumElem = gcsElem.getChild(DimapProductConstants.TAG_HORIZONTAL_DATUM);
            final String datumName = horizDatumElem.getChildTextTrim(DimapProductConstants.TAG_HORIZONTAL_DATUM_NAME);
            final Element ellipsoidElem = horizDatumElem.getChild(DimapProductConstants.TAG_ELLIPSOID);
            final String ellipsoidName = ellipsoidElem.getChildTextTrim(DimapProductConstants.TAG_ELLIPSOID_NAME);
            final Element ellipsoidParamElem = ellipsoidElem.getChild(DimapProductConstants.TAG_ELLIPSOID_PARAMETERS);
            final Element majorAxisElem = ellipsoidParamElem.getChild(DimapProductConstants.TAG_ELLIPSOID_MAJ_AXIS);
            final double majorAxis = Double.parseDouble(majorAxisElem.getTextTrim());
            final Element minorAxisElem = ellipsoidParamElem.getChild(DimapProductConstants.TAG_ELLIPSOID_MIN_AXIS);
            final double minorAxis = Double.parseDouble(minorAxisElem.getTextTrim());

            final Datum datum = new Datum(datumName, new Ellipsoid(ellipsoidName, minorAxis, majorAxis), 0, 0, 0);

            final List geoPosElems = dom.getRootElement().getChildren(DimapProductConstants.TAG_GEOPOSITION);
            final GeoCoding[] geoCodings = new GeoCoding[geoPosElems.size()];

            for (int i = 0; i < geoPosElems.size(); i++) {
                final Element geoPosElem = (Element) geoPosElems.get(i);
                final int bandIndex;
                if (geoPosElems.size() > 1) {
                    bandIndex = Integer.parseInt(geoPosElem.getChildText(DimapProductConstants.TAG_BAND_INDEX));
                } else {
                    bandIndex = 0;
                }

                final Element geoPosInsertElem = geoPosElem.getChild(DimapProductConstants.TAG_GEOPOSITION_INSERT);

                final String ulxString = geoPosInsertElem.getChildTextTrim(DimapProductConstants.TAG_ULX_MAP);
                final float ulX = Float.parseFloat(ulxString);
                final String ulyString = geoPosInsertElem.getChildTextTrim(DimapProductConstants.TAG_ULY_MAP);
                final float ulY = Float.parseFloat(ulyString);
                final String xDimString = geoPosInsertElem.getChildTextTrim(DimapProductConstants.TAG_X_DIM);
                final float xDim = Float.parseFloat(xDimString);
                final String yDimString = geoPosInsertElem.getChildTextTrim(DimapProductConstants.TAG_Y_DIM);
                final float yDim = Float.parseFloat(yDimString);

                final Element simplifiedLMElem = geoPosElem.getChild(
                        DimapProductConstants.TAG_SIMPLIFIED_LOCATION_MODEL);
                final Element directLMElem = simplifiedLMElem.getChild(DimapProductConstants.TAG_DIRECT_LOCATION_MODEL);
                final String dlmOrderString = directLMElem.getAttributeValue(DimapProductConstants.ATTRIB_ORDER);
                final int dlmOrder = Integer.parseInt(dlmOrderString);
                final Element lcListElem = directLMElem.getChild(DimapProductConstants.TAG_LC_LIST);
                final List lcElems = lcListElem.getChildren(DimapProductConstants.TAG_LC);
                final double[] lambdaCoeffs = readCoefficients(lcElems);
                final Element pcListElem = directLMElem.getChild(DimapProductConstants.TAG_PC_LIST);
                final List pcElems = pcListElem.getChildren(DimapProductConstants.TAG_PC);
                final double[] phiCoeffs = readCoefficients(pcElems);

                final Element reverseLMElem = simplifiedLMElem.getChild(
                        DimapProductConstants.TAG_REVERSE_LOCATION_MODEL);
                final String rlmOrderString = reverseLMElem.getAttributeValue(DimapProductConstants.ATTRIB_ORDER);
                final int rlmOrder = Integer.parseInt(rlmOrderString);
                final Element icListElem = reverseLMElem.getChild(DimapProductConstants.TAG_IC_LIST);
                final List icElems = icListElem.getChildren(DimapProductConstants.TAG_IC);
                final double[] xCoeffs = readCoefficients(icElems);
                final Element jcListElem = reverseLMElem.getChild(DimapProductConstants.TAG_JC_LIST);
                final List jcElems = jcListElem.getChildren(DimapProductConstants.TAG_JC);
                final double[] yCoeffs = readCoefficients(jcElems);

                final FXYSum lambdaSum = FXYSum.createFXYSum(dlmOrder, lambdaCoeffs);
                final FXYSum phiSum = FXYSum.createFXYSum(dlmOrder, phiCoeffs);
                final FXYSum xSum = FXYSum.createFXYSum(rlmOrder, xCoeffs);
                final FXYSum ySum = FXYSum.createFXYSum(rlmOrder, yCoeffs);

                geoCodings[bandIndex] = new FXYGeoCoding(ulX, ulY, xDim, yDim, xSum, ySum, phiSum, lambdaSum, datum);
            }
            return geoCodings;
        }

        final String tagCoordRefSys = DimapProductConstants.TAG_COORDINATE_REFERENCE_SYSTEM;
        final Element coordRefSysElem = dom.getRootElement().getChild(tagCoordRefSys);
        if (coordRefSysElem != null) {
            final String tagHorizontalCs = DimapProductConstants.TAG_HORIZONTAL_CS;
            final Element hCsElem = coordRefSysElem.getChild(tagHorizontalCs);
            if (hCsElem != null) {
                final MapInfo mapInfo = createMapInfoSinceDimap1_4_0(hCsElem);
                if (mapInfo != null) {
                    mapInfo.setSceneWidth(product.getSceneRasterWidth());
                    mapInfo.setSceneHeight(product.getSceneRasterHeight());
                    return new GeoCoding[]{new MapGeoCoding(mapInfo)};
                } else {
                    return null;
                }
            } else {
                Debug.trace("DimapProductHelpers.ProductBuilder.createGeoCoding(): " +
                            "the tag <" + tagCoordRefSys + "> contains no tag <" + tagHorizontalCs + ">"); /*I18N*/
            }
            // 1. fallback: try to find a TiePointGeoCoding
            final Element tpgElem = coordRefSysElem.getChild(DimapProductConstants.TAG_GEOCODING_TIE_POINT_GRIDS);
            if (tpgElem != null) {
                final String tpgNameLat = tpgElem.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LAT);
                final String tpgNameLon = tpgElem.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LON);
                TiePointGrid tiePointGridLat;
                TiePointGrid tiePointGridLon;
                if (tpgNameLat != null && tpgNameLon != null) {
                    tiePointGridLat = product.getTiePointGrid(tpgNameLat);
                    tiePointGridLon = product.getTiePointGrid(tpgNameLon);
                    if (tiePointGridLat != null && tiePointGridLon != null) {
                        // TODO - parse datum!!!
                        if (tiePointGridLat.hasRasterData() && tiePointGridLon.hasRasterData()) {
                            return new GeoCoding[]{
                                    new TiePointGeoCoding(tiePointGridLat, tiePointGridLon/*, Datum.WGS_84*/)
                            };
                        } else {
                            Debug.trace(
                                    "DimapProductHelpers.ProductBuilder.createGeoCoding(): tiePoitGrids have no raster data loaded"); /*I18N*/
                        }
                    } else {
                        Debug.trace(
                                "DimapProductHelpers.ProductBuilder.createGeoCoding(): can't find '" + tpgNameLat + "' or '" + tpgNameLon + "'"); /*I18N*/
                    }
                } else {
                    Debug.trace(
                            "DimapProductHelpers.ProductBuilder.createGeoCoding(): missing value for '" + DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LAT + "' or '" + DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LON + "'"); /*I18N*/
                }
            } else {
                Debug.trace("DimapProductHelpers.ProductBuilder.createGeoCoding(): " +
                            "the coordinate reference system tag contains no horizontal coordinat system tag"); /*I18N*/
            }
            // 2. fallback: try to find a MapGeoCoding
            final Element mapElem = coordRefSysElem.getChild(DimapProductConstants.TAG_GEOCODING_MAP);
            if (mapElem != null) {
                final String mapInfoText = mapElem.getChildTextTrim(DimapProductConstants.TAG_GEOCODING_MAP_INFO);
                if (mapInfoText != null) {
                    final String[] strings = StringUtils.toStringArray(mapInfoText, null);
                    if (strings.length >= 9) {
                        final String projectionName = strings[0];
                        final String datumName = strings[7];
                        final MapProjection projection; // new MapProjection(projectionName, transform);
                        projection = MapProjectionRegistry.getProjection(projectionName);
                        if (projection != null) {
                            Datum datum;
                            if (Datum.WGS_84.getName().equalsIgnoreCase(datumName)) {
                                datum = Datum.WGS_84;
                                final float refPixelX = Float.parseFloat(strings[1]);
                                final float refPixelY = Float.parseFloat(strings[2]);
                                final float refPixelEasting = Float.parseFloat(strings[3]);
                                final float refPixelNorthing = Float.parseFloat(strings[4]);
                                final float refPixelWidth = Float.parseFloat(strings[5]);
                                final float refPixelHeight = Float.parseFloat(strings[6]);
                                final MapInfo mapInfo = new MapInfo(projection,
                                                                    refPixelX,
                                                                    refPixelY,
                                                                    refPixelEasting,
                                                                    refPixelNorthing,
                                                                    refPixelWidth,
                                                                    refPixelHeight,
                                                                    datum);
                                if (strings.length == 9) {
                                    mapInfo.setSceneWidth(product.getSceneRasterWidth());
                                    mapInfo.setSceneHeight(product.getSceneRasterHeight());
                                } else if (strings.length == 11) {
                                    mapInfo.setSceneWidth(Integer.parseInt(strings[9]));
                                    mapInfo.setSceneHeight(Integer.parseInt(strings[10]));
                                }
                                return new GeoCoding[]{new MapGeoCoding(mapInfo)};
                            } else {
                                Debug.trace(
                                        "DimapProductHelpers.ProductBuilder.createGeoCoding(): unknown datum '" + datumName + "'"); /*I18N*/
                            }
                        } else {
                            Debug.trace(
                                    "DimapProductHelpers.ProductBuilder.createGeoCoding(): unknown projection '" + projectionName + "'"); /*I18N*/
                        }
                    } else {
                        Debug.trace(
                                "DimapProductHelpers.ProductBuilder.createGeoCoding(): missing map-info parameters"); /*I18N*/
                    }
                } else {
                    Debug.trace(
                            "DimapProductHelpers.ProductBuilder.createGeoCoding(): map-info text is empty"); /*I18N*/
                }
            } else {
                Debug.trace("DimapProductHelpers.ProductBuilder.createGeoCoding(): neither '" /*I18N*/
                            + DimapProductConstants.TAG_GEOCODING_TIE_POINT_GRIDS + "' nor '" /*I18N*/
                            + DimapProductConstants.TAG_GEOCODING_MAP + "' found in '" /*I18N*/
                            + tagCoordRefSys + "' element"); /*I18N*/
            }
        } else {
            Debug.trace("DimapProductHelpers.ProductBuilder.createGeoCoding(): missing '" /*I18N*/
                        + tagCoordRefSys + "' element"); /*I18N*/
        }

        // 3. fallback: try to create a TiePointGeoCoding from "latitude" and "longitude"
        final TiePointGrid tiePointGridLat = product.getTiePointGrid("latitude");
        final TiePointGrid tiePointGridLon = product.getTiePointGrid("longitude");
        if (tiePointGridLat != null && tiePointGridLon != null) {
            // TODO - parse datum!!!
            return new GeoCoding[]{new TiePointGeoCoding(tiePointGridLat, tiePointGridLon/*, Datum.WGS_84*/)};
        } else {
            Debug.trace(
                    "DimapProductHelpers.ProductBuilder.createGeoCoding(): can't find 'latitude' or 'longitude'"); /*I18N*/
        }

        return null;
    }

    private static double[] readCoefficients(final List elementList) {
        final double[] coeffs = new double[elementList.size()];
        for (int i = 0; i < elementList.size(); i++) {
            final Element element = (Element) elementList.get(i);
            final String indexString = element.getAttribute(DimapProductConstants.ATTRIB_INDEX).getValue();
            final int index = Integer.parseInt(indexString);
            coeffs[index] = Double.parseDouble(element.getTextTrim());
        }
        return coeffs;
    }

    private static MapInfo createMapInfoSinceDimap1_4_0(final Element horCsElem) {
        try {
            final Element geogarphicCsElem = horCsElem.getChild(DimapProductConstants.TAG_GEOGRAPHIC_CS);
            final Element horDatumElem = geogarphicCsElem.getChild(DimapProductConstants.TAG_HORIZONTAL_DATUM);
            /**
             *     !!!ATTENTION !!!    -->   Read this!!!!
             *
             * Do not use methods like 'getChildTextTrim' inside this method.
             *
             * Example:
             * final String datumName = horDatumElem.getChildTextTrim(DimapProductConstants.TAG_HORIZONTAL_DATUM_NAME);
             *
             * This methods results a Strings named "null" not a null object if the
             * element is not available in the XML dom.
             *
             * Instead use the folowing sequence:
             * final Element horDatumNameElem = horDatumElem.getChild(DimapProductConstants.TAG_HORIZONTAL_DATUM_NAME);
             * final String datumName = horDatumNameElem.getTextTrim();
             *
             * This sequence throws a NullpointerException if any of the mandatory entries are not exists.
             * This thrown NullpointerException will be catched in this method to trace a log message
             * that this geocoding sequence is malformed.
             */
            final Element horDatumNameElem = horDatumElem.getChild(DimapProductConstants.TAG_HORIZONTAL_DATUM_NAME);
            final String datumName = horDatumNameElem.getTextTrim();
            final Element ellipsoidElem = horDatumElem.getChild(DimapProductConstants.TAG_ELLIPSOID);
            final Element ellipsoidNameElem = ellipsoidElem.getChild(DimapProductConstants.TAG_ELLIPSOID_NAME);
            final String ellipsoidName = ellipsoidNameElem.getTextTrim();
            final Element ellipsParamsElem = ellipsoidElem.getChild(DimapProductConstants.TAG_ELLIPSOID_PARAMETERS);
            final Element semiMinorElem = ellipsParamsElem.getChild(DimapProductConstants.TAG_ELLIPSOID_MIN_AXIS);
            final String semiMinorText = semiMinorElem.getTextTrim();
            final double semiMinor = Double.parseDouble(semiMinorText);
            final Element semiMajorelem = ellipsParamsElem.getChild(DimapProductConstants.TAG_ELLIPSOID_MAJ_AXIS);
            final String semiMajorText = semiMajorelem.getTextTrim();
            final double semiMajor = Double.parseDouble(semiMajorText);

            final Element projectionElem = horCsElem.getChild(DimapProductConstants.TAG_PROJECTION);
            final Element projectionNameElem = projectionElem.getChild(DimapProductConstants.TAG_PROJECTION_NAME);
            final String projectionName = projectionNameElem.getTextTrim();
            final Element projCtMethodElem = projectionElem.getChild(DimapProductConstants.TAG_PROJECTION_CT_METHOD);
            final Element projectionTypeIDElem = projCtMethodElem.getChild(
                    DimapProductConstants.TAG_PROJECTION_CT_NAME);
            final String projectionTypeID = projectionTypeIDElem.getTextTrim();
            final Element projParametersElem = projCtMethodElem.getChild(
                    DimapProductConstants.TAG_PROJECTION_PARAMETERS);
            final List projParamList = projParametersElem.getChildren(DimapProductConstants.TAG_PROJECTION_PARAMETER);
            final Element[] projParams = (Element[]) projParamList.toArray(new Element[projParamList.size()]);
            final double[] parameterValues = new double[projParamList.size()];
            for (int i = 0; i < parameterValues.length; i++) {
                final Element projParam = projParams[i];
                final Element valueTextElem = projParam.getChild(DimapProductConstants.TAG_PROJECTION_PARAMETER_VALUE);
                final String valueText = valueTextElem.getTextTrim();
                parameterValues[i] = Double.parseDouble(valueText);
            }

            final float pixelX;
            final float pixelY;
            final float easting;
            final float northing;
            final float orientation;
            final float pixelSizeX;
            final float pixelSizeY;
            final double noDataValue;
            final String mapUnit;
            final boolean orthorectified;
            final String elevModelName;
            final boolean sceneFitted;
            final int sceneWidth;
            final int sceneHeight;
            final String resamplingName;
            final Element mapInfoElement = horCsElem.getChild(DimapProductConstants.TAG_GEOCODING_MAP_INFO);
            final String mapInfoText = mapInfoElement.getTextTrim();
            if (!StringUtils.isNullOrEmpty(mapInfoText)) {
                final String[] mapInfoStrings = StringUtils.toStringArray(mapInfoText, null);
                final int offs = mapInfoStrings.length % 11;
                pixelX = Float.parseFloat(mapInfoStrings[1 + offs]);
                pixelY = Float.parseFloat(mapInfoStrings[2 + offs]);
                easting = Float.parseFloat(mapInfoStrings[3 + offs]);
                northing = Float.parseFloat(mapInfoStrings[4 + offs]);
                pixelSizeX = Float.parseFloat(mapInfoStrings[5 + offs]);
                pixelSizeY = Float.parseFloat(mapInfoStrings[6 + offs]);
                mapUnit = mapInfoStrings[8 + offs].substring(6);
                orientation = 0.0f;
                noDataValue = MapInfo.DEFAULT_NO_DATA_VALUE;
                elevModelName = null;
                orthorectified = false;
                sceneFitted = false;
                sceneWidth = 0;
                sceneHeight = 0;
                resamplingName = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;
            } else {
                final Element pixelXElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_PIXEL_X);
                pixelX = Float.parseFloat(pixelXElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element pixelYElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_PIXEL_Y);
                pixelY = Float.parseFloat(pixelYElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element eastingElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_EASTING);
                easting = Float.parseFloat(eastingElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element northingElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_NORTHING);
                northing = Float.parseFloat(northingElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element orientationElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_ORIENTATION);
                orientation = Float.parseFloat(orientationElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element pixelSizeXElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_PIXELSIZE_X);
                pixelSizeX = Float.parseFloat(pixelSizeXElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element pixelSizeYElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_PIXELSIZE_Y);
                pixelSizeY = Float.parseFloat(pixelSizeYElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element noDataValueElem = mapInfoElement.getChild(
                        DimapProductConstants.TAG_MAP_INFO_NODATA_VALUE);
                noDataValue = Float.parseFloat(noDataValueElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element mapUnitElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_MAPUNIT);
                mapUnit = mapUnitElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE);
                final Element orthoElement = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_ORTHORECTIFIED);
                orthorectified = Boolean.parseBoolean(
                        orthoElement.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element elevModelElem = mapInfoElement.getChild(
                        DimapProductConstants.TAG_MAP_INFO_ELEVATION_MODEL);
                elevModelName = elevModelElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE).trim();
                final Element sceneFittedElem = mapInfoElement.getChild(
                        DimapProductConstants.TAG_MAP_INFO_SCENE_FITTED);
                sceneFitted = Boolean.parseBoolean(
                        sceneFittedElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element sceneWidthElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_SCENE_WIDTH);
                sceneWidth = Integer.parseInt(sceneWidthElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element sceneHeightElem = mapInfoElement.getChild(
                        DimapProductConstants.TAG_MAP_INFO_SCENE_HEIGHT);
                sceneHeight = Integer.parseInt(sceneHeightElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE));
                final Element resamplingElem = mapInfoElement.getChild(DimapProductConstants.TAG_MAP_INFO_RESAMPLING);
                resamplingName = resamplingElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE);
            }

            final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(projectionTypeID);
            final MapTransform mapTransform = descriptor.createTransform(parameterValues);
            final MapProjection projection = new MapProjection(projectionName, mapTransform, mapUnit);
            final Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName, semiMinor, semiMajor);
            final Datum datum = new Datum(datumName, ellipsoid, 0, 0, 0); // @todo nf/nf - read also DX,DY,DZ
            final MapInfo mapInfo = new MapInfo(projection, pixelX, pixelY, easting, northing, pixelSizeX, pixelSizeY,
                                                datum);
            mapInfo.setOrientation(orientation);
            mapInfo.setNoDataValue(noDataValue);
            mapInfo.setOrthorectified(orthorectified);
            mapInfo.setElevationModelName(elevModelName);
            mapInfo.setSceneSizeFitted(sceneFitted);
            mapInfo.setSceneWidth(sceneWidth);
            mapInfo.setSceneHeight(sceneHeight);
            final Resampling resampling = ResamplingFactory.createResampling(resamplingName);
            if (resampling != null) {
                mapInfo.setResampling(resampling);
            } else {
                Debug.trace("Unknown resampling: '" + resamplingName + "'");
            }
            return mapInfo;
        } catch (Exception e) {
            Debug.trace("Malformed geocoding");
            Debug.trace(e);
            return null;
        }
    }

    private static String getTiePointGridIndex(Element rootElement, String tiePointGridName) {
        Element tiePointGrids = rootElement.getChild(DimapProductConstants.TAG_TIE_POINT_GRIDS);
        if (tiePointGrids != null) {
            final List tiePointGridInfos = tiePointGrids.getChildren(DimapProductConstants.TAG_TIE_POINT_GRID_INFO);
            for (Object child : tiePointGridInfos) {
                final Element tiePointGridInfo = (Element) child;
                final String actualTiePointGridName = tiePointGridInfo.getChildTextTrim(
                        DimapProductConstants.TAG_TIE_POINT_GRID_NAME);
                if (tiePointGridName.equals(actualTiePointGridName)) {
                    return tiePointGridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_INDEX);
                }
            }
        }
        return null;
    }

    private static String getBandName(Element rootElement, String index) {
        Element imageInterpretation = rootElement.getChild(DimapProductConstants.TAG_IMAGE_INTERPRETATION);
        if (imageInterpretation != null) {
            final List spectralBandInfos = imageInterpretation.getChildren(
                    DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
            for (Object child : spectralBandInfos) {
                Element bandInfo = (Element) child;
                final String actualBandIndex = bandInfo.getChildTextTrim(DimapProductConstants.TAG_BAND_INDEX);
                if (index.equals(actualBandIndex)) {
                    return bandInfo.getChildTextTrim(DimapProductConstants.TAG_BAND_NAME);
                }
            }
        }
        return null;
    }

    private static String getTiePointGridName(Element rootElement, String index) {
        Element tiePointGrids = rootElement.getChild(DimapProductConstants.TAG_TIE_POINT_GRIDS);
        if (tiePointGrids != null) {
            final List tiePointGridInfos = tiePointGrids.getChildren(DimapProductConstants.TAG_TIE_POINT_GRID_INFO);
            for (Object child : tiePointGridInfos) {
                final Element tiePointGridInfo = (Element) child;
                final String currentTiePointGridIndex = tiePointGridInfo.getChildTextTrim(
                        DimapProductConstants.TAG_TIE_POINT_GRID_INDEX);
                if (index.equals(currentTiePointGridIndex)) {
                    return tiePointGridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_NAME);
                }
            }
        }
        return null;
    }

    /**
     * Creates a {@link FileFilter} for BEAM-DIMAP files.
     *
     * @return a FileFilter for use with a {@link javax.swing.JFileChooser}
     *
     * @see org.esa.beam.util.io.BeamFileChooser
     */
    public static FileFilter createDimapFileFilter() {
        return new DimapFileFilter();
    }

    // todo - move - only used in tests (nf)
    /**
     * Creates a parsed {@link Document} from the given {@link InputStream inputStream}.
     *
     * @param inputStream the stream to be parsed
     *
     * @return a parsed inputStream
     */
    public static Document createDom(final InputStream inputStream) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        org.w3c.dom.Document w3cDocument = null;
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            w3cDocument = builder.parse(inputStream);
        } catch (ParserConfigurationException e) {
            Debug.trace(e);
        } catch (SAXException e) {
            Debug.trace(e);
        } catch (IOException e) {
            Debug.trace(e);
        }
        return new DOMBuilder().build(w3cDocument);
    }

    /**
     * Converts a given BEAM <code>unit</code> into a DIMAP conform unit.
     *
     * @param unit a BEAM unit
     *
     * @return the converted unit
     *
     * @see DimapProductHelpers#convertDimapUnitToBeamUnit(String)
     */
    public static String convertBeamUnitToDimapUnit(final String unit) {
        final String unitLowerCase = unit.toLowerCase();
        if (unitLowerCase.startsWith("meter")) {
            return "M";
        } else if (unitLowerCase.startsWith("kilometer")) {
            return "KM";
        } else if (unitLowerCase.startsWith("deg")) {
            return "DEG";
        } else if (unitLowerCase.startsWith("rad")) {
            return "RAD";
        }
        return unitLowerCase;
    }

    /**
     * Converts a given DIMAP <code>unit</code> into a BEAM conform unit.
     *
     * @param unit a DIMAP unit
     *
     * @return the converted unit
     *
     * @see DimapProductHelpers#convertBeamUnitToDimapUnit(String)
     */
    public static String convertDimapUnitToBeamUnit(final String unit) {
        if (unit.equals("M")) {
            return "meter";
        } else if (unit.equals("KM")) {
            return "kilometer";
        } else if (unit.equals("DEG")) {
            return "deg";
        } else if (unit.equals("RAD")) {
            return "rad";
        }
        return unit;
    }

    private static class ProductBuilder {

        private final Document _dom;

        private ProductBuilder(Document dom) {
            _dom = dom;
        }

        private Document getDom() {
            return _dom;
        }

        private Product createProduct() {
            final Product product = new Product(getProductName(), getProductType(), getSceneRasterWidth(),
                                                getSceneRasterHeight());
            setSceneRasterStartAndStopTime(product);
            setDescription(product);
            addBitmaskDefinitions(product);
            addFlagsCoding(product);
            addIndexCoding(product);
            addBands(product);
            addTiePointGrids(product);
            addDisplayInfosToBandsAndTiePointGrids(product);
            addAnnotationDataset(product);
            addPins(product);
            addGcps(product);
            addPointingFactory(product);
            return product;
        }

        private static void addPointingFactory(final Product product) {
            PointingFactoryRegistry registry = PointingFactoryRegistry.getInstance();
            PointingFactory pointingFactory = registry.getPointingFactory(product.getProductType());
            product.setPointingFactory(pointingFactory);

        }

        private void addPins(Product product) {
            Element pinGroup = getRootElement().getChild(DimapProductConstants.TAG_PIN_GROUP);
            List pinElements;
            if (pinGroup != null) {
                pinElements = pinGroup.getChildren(DimapProductConstants.TAG_PLACEMARK);
            } else {
                // get pins of old DIMAP files prior version 2.3
                pinElements = getRootElement().getChildren(DimapProductConstants.TAG_PIN);
            }
            for (Object pinElement : pinElements) {
                final Element pinElem = (Element) pinElement;
                final Pin pin = Pin.createPin(pinElem);
                if (pin != null) {
                    pin.updatePixelPos(product.getGeoCoding());
                    product.getPinGroup().add(pin);
                }
            }
        }

        private void addGcps(Product product) {
            Element gcpGroupElement = getRootElement().getChild(DimapProductConstants.TAG_GCP_GROUP);
            List gcpElements;
            if (gcpGroupElement != null) {
                gcpElements = gcpGroupElement.getChildren(DimapProductConstants.TAG_PLACEMARK);
            } else {
                gcpElements = Collections.EMPTY_LIST;
            }
            for (Object gcpElement : gcpElements) {
                final Element gcpElem = (Element) gcpElement;
                final Pin gcp = Pin.createGcp(gcpElem);
                if (gcp != null) {
                    product.getGcpGroup().add(gcp);
                }
            }
        }

        private void addAnnotationDataset(Product product) {
            final MetadataElement mdElem = product.getMetadataRoot();
            if (mdElem == null) {
                return;
            }
            final Element datasetSourcesElem = getRootElement().getChild(DimapProductConstants.TAG_DATASET_SOURCES);
            if (datasetSourcesElem == null) {
                return;
            }
            final Element element = datasetSourcesElem.getChild(DimapProductConstants.TAG_METADATA_ELEMENT);
            if (element == null) {
                return;
            }
            mdElem.setDescription(element.getAttributeValue(DimapProductConstants.ATTRIB_DESCRIPTION));
            addMetadataAttributes(element, mdElem);
            addMetadataElements(element, mdElem);
        }

        private static void addMetadataElements(final Element element, MetadataElement mdElem) {
            final List metadataElements = element.getChildren(DimapProductConstants.TAG_METADATA_ELEMENT);
            for (Object child : metadataElements) {
                final Element metadataElement = (Element) child;

                final String elemName = metadataElement.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
                if (elemName == null || elemName.length() == 0) {
                    continue;
                }

                final MetadataElement newMdElem = new MetadataElement(elemName);
                newMdElem.setDescription(metadataElement.getAttributeValue(DimapProductConstants.ATTRIB_DESCRIPTION));
                addMetadataAttributes(metadataElement, newMdElem);
                addMetadataElements(metadataElement, newMdElem);
                mdElem.addElement(newMdElem);
            }
        }

        private static void addMetadataAttributes(final Element element, MetadataElement mdElem) {
            final List attributeElements = element.getChildren(DimapProductConstants.TAG_METADATA_ATTRIBUTE);
            for (Object child : attributeElements) {
                final Element attribElement = (Element) child;

                final String attName = attribElement.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
                if (attName == null || attName.length() == 0) {
                    continue;
                }

                final String attType = attribElement.getAttributeValue(DimapProductConstants.ATTRIB_TYPE);
                if (attType == null || attType.length() == 0) {
                    continue;
                }
                final int type = ProductData.getType(attType);

                final String attValue = attribElement.getTextTrim();
                if (attValue == null || attValue.length() == 0) {
                    continue;
                }
                final ProductData data;
                if (type == ProductData.TYPE_ASCII) {
                    data = ProductData.createInstance(attValue);
                } else if (type == ProductData.TYPE_UTC) {
                    if (attValue.contains(",")) {
                        // *************************************************
                        // this case is necesarry for backward compatibility
                        // *************************************************
                        final String[] dataValues = StringUtils.csvToArray(attValue);
                        data = ProductData.createInstance(type);
                        data.setElems(dataValues);
                    } else {
                        ProductData.UTC utc = null;
                        try {
                            utc = ProductData.UTC.parse(attValue);
                        } catch (ParseException e) {
                            Debug.trace(e);
                        } finally {
                            data = utc;
                        }
                    }
                } else if (ProductData.isUIntType(type) && attValue.contains("-")) {
                    // *************************************************
                    // this case is necesarry for backward compatibility
                    // *************************************************
                    final String[] dataValues = StringUtils.csvToArray(attValue);
                    final int length = dataValues.length;
                    final Object elems;
                    if (type == ProductData.TYPE_UINT8) {
                        final byte[] bytes = new byte[length];
                        for (int i = 0; i < length; i++) {
                            bytes[i] = Byte.parseByte(dataValues[i]);
                        }
                        elems = bytes;
                    } else if (type == ProductData.TYPE_UINT16) {
                        final short[] shorts = new short[length];
                        for (int i = 0; i < length; i++) {
                            shorts[i] = Short.parseShort(dataValues[i]);
                        }
                        elems = shorts;
                    } else {
                        final int[] ints = new int[length];
                        for (int i = 0; i < length; i++) {
                            ints[i] = Integer.parseInt(dataValues[i]);
                        }
                        elems = ints;
                    }
                    data = ProductData.createInstance(type, length);
                    data.setElems(elems);
                } else {
                    final String[] dataValues = StringUtils.csvToArray(attValue);
                    final int length = dataValues.length;
                    data = ProductData.createInstance(type, length);
                    data.setElems(dataValues);
                }
                if (data == null) {
                    continue;
                }

                final boolean readOnly = !"rw".equalsIgnoreCase(
                        attribElement.getAttributeValue(DimapProductConstants.ATTRIB_MODE));
                final MetadataAttribute metadataAttribute = new MetadataAttribute(attName, data, readOnly);

                metadataAttribute.setDescription(
                        attribElement.getAttributeValue(DimapProductConstants.ATTRIB_DESCRIPTION));
                metadataAttribute.setUnit(attribElement.getAttributeValue(DimapProductConstants.ATTRIB_UNIT));
                mdElem.addAttribute(metadataAttribute);
            }
        }

        private void addDisplayInfosToBandsAndTiePointGrids(Product product) {
            final List imageDisplayElems = getRootElement().getChildren(DimapProductConstants.TAG_IMAGE_DISPLAY);
            for (Object child : imageDisplayElems) {
                final Element imageDisplayElem = (Element) child;
                addBandStatistics(imageDisplayElem, product);
                addBitmaskOverlays(imageDisplayElem, product);
                addRoiDefinitions(imageDisplayElem, product);
            }
        }

        private void addRoiDefinitions(Element imageDisplayElem, Product product) {
            final List roiDefinitions = imageDisplayElem.getChildren(DimapProductConstants.TAG_ROI_DEFINITION);
            for (Object child : roiDefinitions) {
                final Element roiDefinitionElem = (Element) child;
                final ROIDefinition roiDefinition = new ROIDefinition();
                roiDefinition.initFromJDOMElem(roiDefinitionElem);
                final String index = roiDefinitionElem.getChildTextTrim(DimapProductConstants.TAG_BAND_INDEX);
                final String bandName = getBandName(getRootElement(), index);
                final Band band = product.getBand(bandName);
                if (band != null) {
                    band.setROIDefinition(roiDefinition);
                }
            }
        }

        private void addBitmaskOverlays(Element imageDisplayElem, Product product) {
            final List overlays = imageDisplayElem.getChildren(DimapProductConstants.TAG_BITMASK_OVERLAY);
            for (Object overlay : overlays) {
                final Element overlayElem = (Element) overlay;
                final Element bitmaskNamesElem = overlayElem.getChild(DimapProductConstants.TAG_BITMASK);
                String namesCSV = null;
                if (bitmaskNamesElem != null) {
                    namesCSV = bitmaskNamesElem.getAttributeValue(DimapProductConstants.ATTRIB_NAMES);
                }
                String[] names = null;
                if (namesCSV != null && namesCSV.length() > 0) {
                    names = StringUtils.csvToArray(namesCSV);
                }
                if (names != null) {
                    BitmaskOverlayInfo bitmaskOverlayInfo = null;
                    final Element bandIndexElem = overlayElem.getChild(DimapProductConstants.TAG_BAND_INDEX);
                    if (bandIndexElem != null) {
                        final String bandName = getBandName(getRootElement(), bandIndexElem.getTextTrim());
                        if (bandName != null) {
                            final Band band = product.getBand(bandName);
                            if (band != null) {
                                bitmaskOverlayInfo = band.getBitmaskOverlayInfo();
                                if (bitmaskOverlayInfo == null) {
                                    bitmaskOverlayInfo = new BitmaskOverlayInfo();
                                    band.setBitmaskOverlayInfo(bitmaskOverlayInfo);
                                }
                            }
                        }
                    }
                    if (bitmaskOverlayInfo == null) {
                        final Element tiePointGridIndexElem = overlayElem.getChild(
                                DimapProductConstants.TAG_TIE_POINT_GRID_INDEX);
                        if (tiePointGridIndexElem != null) {
                            final String tiePointGridName = getTiePointGridName(getRootElement(),
                                                                                tiePointGridIndexElem.getTextTrim());
                            if (tiePointGridName != null) {
                                final TiePointGrid tiePointGrid = product.getTiePointGrid(tiePointGridName);
                                if (tiePointGrid != null) {
                                    bitmaskOverlayInfo = tiePointGrid.getBitmaskOverlayInfo();
                                    if (bitmaskOverlayInfo == null) {
                                        bitmaskOverlayInfo = new BitmaskOverlayInfo();
                                        tiePointGrid.setBitmaskOverlayInfo(bitmaskOverlayInfo);
                                    }
                                }
                            }
                        }
                    }
                    if (bitmaskOverlayInfo != null) {
                        for (String name : names) {
                            final BitmaskDef def = product.getBitmaskDef(name);
                            bitmaskOverlayInfo.addBitmaskDef(def);
                        }
                    }
                }
            }
        }

        private void addBandStatistics(Element imageDisplayElem, Product product) {
            final List bandStatisticsElems = imageDisplayElem.getChildren(DimapProductConstants.TAG_BAND_STATISTICS);
            for (Object bandStatistics : bandStatisticsElems) {
                final Element bandStatisticsElem = (Element) bandStatistics;
                final String bandIndex = bandStatisticsElem.getChildTextTrim(DimapProductConstants.TAG_BAND_INDEX);
                final String bandName = getBandName(getRootElement(), bandIndex);
                if (bandName != null) {
                    final Band band = product.getBand(bandName);
                    if (band != null) {
                        Stx stx = createStx(band, bandStatisticsElem);
                        if (stx != null) {
                            band.setStx(stx);
                        }
                        band.setImageInfo(createImageInfo(bandStatisticsElem));
                    }
                }
            }
        }

        private static Stx createStx(Band band, Element bandStatisticsElem) {
            final Double minSample = getElemDouble(bandStatisticsElem, DimapProductConstants.TAG_STX_MIN);
            final Double maxSample = getElemDouble(bandStatisticsElem, DimapProductConstants.TAG_STX_MAX);
            final Double mean = getElemDouble(bandStatisticsElem, DimapProductConstants.TAG_STX_MEAN);
            final Double stdDev = getElemDouble(bandStatisticsElem, DimapProductConstants.TAG_STX_STDDEV);
            final Integer level = getElemInt(bandStatisticsElem, DimapProductConstants.TAG_STX_LEVEL);
            final int[] bins = getHistogramBins(bandStatisticsElem);
            if (minSample != null && maxSample != null) {
                return new Stx(band.scaleInverse(minSample),
                               band.scaleInverse(maxSample),
                               mean != null ? band.scaleInverse(mean) : Double.NaN,
                               stdDev != null ? band.scaleInverse(stdDev) : Double.NaN,
                               ProductData.isIntType(band.getDataType()),
                               bins == null ? new int[0] : bins,
                               level == null ? 0 : level);
            }
            return null;
        }

        private static ImageInfo createImageInfo(Element bandStatisticsElem) {
            final ColorPaletteDef.Point[] points = getColorPalettePoints(bandStatisticsElem);
            final int numColors = getNumColors(bandStatisticsElem);
            final ImageInfo imageInfo = new ImageInfo(new ColorPaletteDef(points, numColors));
            final Element noDataElem = bandStatisticsElem.getChild(DimapProductConstants.TAG_NO_DATA_COLOR);
            if (noDataElem != null) {
                imageInfo.setNoDataColor(createColor(noDataElem));
            }
            final Element histomElem = bandStatisticsElem.getChild(DimapProductConstants.TAG_HISTOGRAM_MATCHING);
            if (histomElem != null && histomElem.getValue() != null) {
                imageInfo.setHistogramMatching(ImageInfo.getHistogramMatching(histomElem.getValue()));
            }
            return imageInfo;
        }

        private static Double getElemDouble(Element element, String tag) {
            final String text = element.getChildTextTrim(tag);
            if (text != null) {
                try {
                    return Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().severe(
                            "Number format exception at reading DIMAP product tag '" + tag + "'");
                    BeamLogManager.getSystemLogger().log(Level.SEVERE,
                                                         "Number format exception at reading DIMAP product tag '" + tag + "'",
                                                         e);
                    return null;
                }
            } else {
                return null;
            }
        }

        private static Integer getElemInt(Element element, String tag) {
            final String text = element.getChildTextTrim(tag);
            if (text != null) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().severe(
                            "Number format exception at reading DIMAP product tag '" + tag + "'");
                    BeamLogManager.getSystemLogger().log(Level.SEVERE,
                                                         "Number format exception at reading DIMAP product tag '" + tag + "'",
                                                         e);
                    return null;
                }
            } else {
                return null;
            }
        }

        private static int getNumColors(Element bandStatisticsElem) {
            int numColors = 256;
            try {
                numColors = Integer.parseInt(bandStatisticsElem.getChildTextTrim(DimapProductConstants.TAG_NUM_COLORS));
            } catch (NumberFormatException e) {
                Debug.trace(e);
            }
            return numColors;
        }

        private static int[] getHistogramBins(Element bandStatisticsElem) {
            final String histogramValues = bandStatisticsElem.getChildTextTrim(DimapProductConstants.TAG_HISTOGRAM);
            if (StringUtils.isNullOrEmpty(histogramValues)) {
                return null;
            }
            return StringUtils.toIntArray(histogramValues, null);
        }

        private static ColorPaletteDef.Point[] getColorPalettePoints(Element bandStatisticsElem) {
            final List colorPalettePointElems = bandStatisticsElem.getChildren(
                    DimapProductConstants.TAG_COLOR_PALETTE_POINT);
            ColorPaletteDef.Point[] points = null;
            if (colorPalettePointElems.size() > 1) {
                final Iterator iteratorCPPE = colorPalettePointElems.iterator();
                points = new ColorPaletteDef.Point[colorPalettePointElems.size()];
                for (int i = 0; i < points.length; i++) {
                    final Element colorPalettePointElem = (Element) iteratorCPPE.next();
                    final Color color = DimapProductHelpers.createColor(
                            colorPalettePointElem.getChild(DimapProductConstants.TAG_COLOR));
                    final double sample = getSample(colorPalettePointElem);
                    final String label = getLabel(colorPalettePointElem);
                    points[i] = new ColorPaletteDef.Point(sample, color, label);
                }
            }
            return points;
        }

        private static double getSample(Element colorPalettePointElem) {
            final Element sampleElem = colorPalettePointElem.getChild(DimapProductConstants.TAG_SAMPLE);
            if (sampleElem != null) {
                return Double.parseDouble(sampleElem.getTextTrim());
            }
            return 0;
        }

        private static String getLabel(Element colorPalettePointElem) {
            final Element labelElem = colorPalettePointElem.getChild(DimapProductConstants.TAG_LABEL);
            if (labelElem != null) {
                return labelElem.getTextTrim();
            }
            return "";
        }

        private void addTiePointGrids(Product product) {
            final Element parent = getRootElement().getChild(DimapProductConstants.TAG_TIE_POINT_GRIDS);
            if (parent != null) {
                final List children = parent.getChildren(DimapProductConstants.TAG_TIE_POINT_GRID_INFO);
                for (Object child : children) {
                    final Element gridInfo = (Element) child;
                    final String name = gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_GRID_NAME);
                    final int width = Integer.parseInt(
                            gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_NCOLS));
                    final int height = Integer.parseInt(
                            gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_NROWS));
                    final float offsX = Float.parseFloat(
                            gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_OFFSET_X));
                    final float offsY = Float.parseFloat(
                            gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_OFFSET_Y));
                    final float subsX = Float.parseFloat(
                            gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_STEP_X));
                    final float subsY = Float.parseFloat(
                            gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_STEP_Y));
                    final float[] floats = new float[width * height];
                    boolean cyclic = false;
                    final String cyclicText = gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_CYCLIC);
                    if (cyclicText != null) {
                        cyclic = Boolean.parseBoolean(cyclicText);
                    }
                    final TiePointGrid tiePointGrid;
                    if (cyclic) {
                        tiePointGrid = new TiePointGrid(name, width, height, offsX, offsY, subsX, subsY, floats,
                                                        TiePointGrid.DISCONT_AT_180);
                    } else {
                        tiePointGrid = new TiePointGrid(name, width, height, offsX, offsY, subsX, subsY, floats);
                    }
                    tiePointGrid.setDescription(
                            gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_DESCRIPTION));
                    tiePointGrid.setUnit(gridInfo.getChildTextTrim(DimapProductConstants.TAG_TIE_POINT_PHYSICAL_UNIT));
                    product.addTiePointGrid(tiePointGrid);
                }
            }
        }

        private void addFlagsCoding(Product product) {
            addSampleCoding(product,
                            DimapProductConstants.TAG_FLAG_CODING,
                            DimapProductConstants.TAG_FLAG,
                            DimapProductConstants.TAG_FLAG_NAME,
                            DimapProductConstants.TAG_FLAG_INDEX,
                            DimapProductConstants.TAG_FLAG_DESCRIPTION);
        }

        private void addIndexCoding(Product product) {
            addSampleCoding(product,
                            DimapProductConstants.TAG_INDEX_CODING,
                            DimapProductConstants.TAG_INDEX,
                            DimapProductConstants.TAG_INDEX_NAME,
                            DimapProductConstants.TAG_INDEX_VALUE,
                            DimapProductConstants.TAG_INDEX_DESCRIPTION);
        }

        private void addSampleCoding(Product product,
                                     String tagFlagCoding,
                                     String tagFlag,
                                     String tagFlagName,
                                     String tagFlagIndex,
                                     String tagFlagDescription) {
            final List children = getRootElement().getChildren(tagFlagCoding);
            for (Object aChildren : children) {
                final Element flagCodingElem = (Element) aChildren;
                final String codingName = flagCodingElem.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
                final SampleCoding sampleCoding;
                if (tagFlag.equals(DimapProductConstants.TAG_INDEX)) {
                    final IndexCoding indexCoding = new IndexCoding(codingName);
                    product.getIndexCodingGroup().add(indexCoding);
                    sampleCoding = indexCoding;
                } else {
                    final FlagCoding flagCoding = new FlagCoding(codingName);
                    product.getFlagCodingGroup().add(flagCoding);
                    sampleCoding = flagCoding;
                }
                addSamples(tagFlag, tagFlagName, tagFlagIndex, tagFlagDescription, flagCodingElem, sampleCoding);
            }
        }

        private void addSamples(String tagList, String tagName, String tagValue, String tagDescription,
                                Element sampleCodingElement, SampleCoding sampleCoding) {
            final List list = sampleCodingElement.getChildren(tagList);
            for (Object o : list) {
                final Element element = (Element) o;
                final String name = element.getChildTextTrim(tagName);
                final int value = Integer.parseInt(element.getChildTextTrim(tagValue));
                final String description = element.getChildTextTrim(tagDescription);
                sampleCoding.addSample(name, value, description);
            }
        }


        private void addBitmaskDefinitions(Product product) {
            final Element bitmaskDefs = getRootElement().getChild(DimapProductConstants.TAG_BITMASK_DEFINITIONS);
            List bitmaskDefList;
            if (bitmaskDefs != null) {
                bitmaskDefList = bitmaskDefs.getChildren(DimapProductConstants.TAG_BITMASK_DEFINITION);
            } else {
                bitmaskDefList = getRootElement().getChildren(DimapProductConstants.TAG_BITMASK_DEFINITION);
            }
            for (Object child : bitmaskDefList) {
                final Element bitmaskDefElem = (Element) child;
                final BitmaskDef bitmaskDef = BitmaskDef.createBitmaskDef(bitmaskDefElem);
                if (bitmaskDef != null) {
                    product.addBitmaskDef(bitmaskDef);
                }
            }
        }

        private void addBands(Product product) {
            final Element child = getRootElement().getChild(DimapProductConstants.TAG_IMAGE_INTERPRETATION);
            if (child != null) {
                addSpectralBands(child, product);
            }
        }

        private static void addSpectralBands(final Element parent, Product product) {
            final List children = parent.getChildren(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
            final List<Element> filterBandElementList = new ArrayList<Element>();
            for (Object child : children) {
                final Element element = (Element) child;
                if (isFilterBand(element)) {
                    // filter bands must be added as the last bands
                    // they need an already existing RasterDataNode as source
                    filterBandElementList.add(element);
                } else {
                    final Band band = addBand(element, product);
                    setGeneralBandProperties(band, element, product);
                }
            }
            for (Object child : filterBandElementList) {
                final Element element = (Element) child;
                final Band band = addBand(element, product);
                setGeneralBandProperties(band, element, product);
            }
        }

        private static void setGeneralBandProperties(Band band, Element element, Product product) {
            if (band != null) {
                final String validMaskExpression = getValidMaskExpression(element);
                if (validMaskExpression != null && validMaskExpression.trim().length() > 0) {
                    band.setValidPixelExpression(validMaskExpression);
                }
                setUnit(element, band);
                setSpectralWaveLength(element, band);
                setSpectralBandWidth(element, band);
                setSolarFlux(element, band);
                setSpectralBandIndex(element, band);
                setScaling(element, band);
                setFlagCoding(element, band, product);
                setIndexCoding(element, band, product);
                setNoDataValueUsed(element, band);
                setNoDataValue(element, band);
            }
        }

        private static String getValidMaskExpression(Element element) {
            return element.getChildTextTrim(DimapProductConstants.TAG_VALID_MASK_TERM);
        }

        private static void setFlagCoding(final Element element, final Band band, Product product) {
            final String codingName = element.getChildTextTrim(DimapProductConstants.TAG_FLAG_CODING_NAME);
            if (codingName != null) {
                final FlagCoding flagCoding = product.getFlagCodingGroup().get(codingName);
                if (flagCoding != null) {
                    band.setSampleCoding(flagCoding);
                }
            }
        }

        private static void setIndexCoding(Element element, Band band, Product product) {
            final String codingName = element.getChildTextTrim(DimapProductConstants.TAG_INDEX_CODING_NAME);
            if (codingName != null) {
                final IndexCoding indexCoding = product.getIndexCodingGroup().get(codingName);
                if (indexCoding != null) {
                    band.setSampleCoding(indexCoding);
                }
            }
        }

        private static void setSolarFlux(final Element element, final Band band) {
            final String solarFlux = element.getChildTextTrim(DimapProductConstants.TAG_SOLAR_FLUX);
            if (solarFlux != null) {
                band.setSolarFlux(Float.parseFloat(solarFlux));
            }
        }

        private static void setSpectralBandIndex(final Element element, final Band band) {
            final String spectralBandIndex = element.getChildTextTrim(DimapProductConstants.TAG_SPECTRAL_BAND_INDEX);
            if (spectralBandIndex != null) {
                band.setSpectralBandIndex(Integer.parseInt(spectralBandIndex));
            } else {
                band.setSpectralBandIndex(-1);
            }
        }

        private static void setScaling(final Element element, final Band band) {
            final String scalingFactorString = element.getChildTextTrim(DimapProductConstants.TAG_SCALING_FACTOR);
            if (scalingFactorString != null) {
                band.setScalingFactor(Double.parseDouble(scalingFactorString));
            }
            final String scalingOffsetString = element.getChildTextTrim(DimapProductConstants.TAG_SCALING_OFFSET);
            if (scalingOffsetString != null) {
                band.setScalingOffset(Double.parseDouble(scalingOffsetString));
            }
            final String log10ScaledString = element.getChildTextTrim(DimapProductConstants.TAG_SCALING_LOG_10);
            if (log10ScaledString != null) {
                band.setLog10Scaled(Boolean.parseBoolean(log10ScaledString));
            }
        }

        private static Band addBand(final Element element, Product product) {
            Band band = null;
            final String bandName = element.getChildTextTrim(DimapProductConstants.TAG_BAND_NAME);
            final String description = element.getChildTextTrim(DimapProductConstants.TAG_BAND_DESCRIPTION);
            final int type = ProductData.getType(element.getChildTextTrim(DimapProductConstants.TAG_DATA_TYPE));
            if (type == ProductData.TYPE_UNDEFINED) {
                return null;
            }
            if (isVirtualBand(element)) {
                final int width = product.getSceneRasterWidth();
                final int height = product.getSceneRasterHeight();
                final VirtualBand virtualBand = new VirtualBand(bandName, type, width, height, getExpression(element));
                product.addBand(virtualBand);
                virtualBand.setNoDataValue(getInvalidValue(element));
                virtualBand.setNoDataValueUsed(getUseInvalidValue(element));
                band = virtualBand;
            } else if (isFilterBand(element)) {
                final DimapPersistable persistable = DimapPersistence.getPersistable(element);
                if (persistable != null) {
                    band = (Band) persistable.createObjectFromXml(element, product);
                    // currently it can be null if the operator of filtered band is of type
                    // GeneralFilterBand.STDDEV or GeneralFilterBand.RMS
                    if (band != null) {
                        product.addBand(band);
                    }
                }
            } else {
                product.addBand(bandName, type);
                band = product.getBand(bandName);
            }
            if (band != null) {
                band.setDescription(description);
            }
            return band;
        }

        private static float getInvalidValue(Element element) {
            return getFloatValue(element, DimapProductConstants.TAG_VIRTUAL_BAND_INVALID_VALUE);
        }

        private static float getFloatValue(Element element, final String key) {
            String sfloat = null;
            if (element != null) {
                sfloat = element.getChildTextTrim(key);
            }
            if (sfloat != null) {
                try {
                    return Float.parseFloat(sfloat);
                } catch (NumberFormatException e) {
                    Debug.trace(e);
                    return 0;
                }
            }
            return 0;
        }

        private static String getExpression(Element element) {
            if (element != null) {
                return element.getChildTextTrim(DimapProductConstants.TAG_VIRTUAL_BAND_EXPRESSION);
            }
            return null;
        }

        private static boolean getUseInvalidValue(Element element) {
            return is(element, DimapProductConstants.TAG_VIRTUAL_BAND_USE_INVALID_VALUE);
        }

        private static boolean getCheckInvalids(Element element) {
            return is(element, DimapProductConstants.TAG_VIRTUAL_BAND_CHECK_INVALIDS);
        }

        private static boolean isVirtualBand(Element element) {
            return is(element, DimapProductConstants.TAG_VIRTUAL_BAND);
        }

        private static boolean isFilterBand(Element element) {
            return element != null && element.getChild(DimapProductConstants.TAG_FILTER_BAND_INFO) != null;
        }

        private static boolean is(Element element, final String tagName) {
            if (element == null) {
                return false;
            }
            final String childTextTrim = element.getChildTextTrim(tagName);
            return Boolean.parseBoolean(childTextTrim);
        }

        private static void setSpectralWaveLength(final Element element, final Band band) {
            final String bandWavelen = element.getChildTextTrim(DimapProductConstants.TAG_BAND_WAVELEN);
            if (bandWavelen != null) {
                band.setSpectralWavelength(Float.parseFloat(bandWavelen));
            }
        }

        private static void setSpectralBandWidth(final Element element, final Band band) {
            final String bandWidth = element.getChildTextTrim(DimapProductConstants.TAG_BANDWIDTH);
            if (bandWidth != null) {
                band.setSpectralBandwidth(Float.parseFloat(bandWidth));
            }
        }

        private static void setNoDataValue(final Element element, final Band band) {
            final String noDataValue = element.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE);
            if (noDataValue != null) {
                band.setNoDataValue(Double.parseDouble(noDataValue));
            }
        }

        private static void setNoDataValueUsed(final Element element, final Band band) {
            final String noDataValueUsed = element.getChildTextTrim(DimapProductConstants.TAG_NO_DATA_VALUE_USED);
            if (noDataValueUsed != null) {
                band.setNoDataValueUsed(Boolean.parseBoolean(noDataValueUsed));
            }
        }

        private static void setUnit(final Element element, final Band band) {
            final String unit = element.getChildTextTrim(DimapProductConstants.TAG_PHYSICAL_UNIT);
            if (unit != null) {
                band.setUnit(unit);
            }
        }

        private String getProductName() {
            final Element child = getRootElement().getChild(DimapProductConstants.TAG_DATASET_ID);
            if (child != null) {
                return child.getChildTextTrim(DimapProductConstants.TAG_DATASET_NAME);
            } else {
                return "";
            }
        }

        private void setDescription(Product product) {
            if (product == null) {
                return;
            }
            String description = null;
            Element child = getRootElement().getChild(DimapProductConstants.TAG_DATASET_USE);
            if (child != null) {
                description = child.getChildTextTrim(DimapProductConstants.TAG_DATASET_COMMENTS);
            }
            if (description == null || description.length() == 0) {
                child = getRootElement().getChild(DimapProductConstants.TAG_DATASET_ID);
                if (child != null) {
                    description = child.getChildTextTrim(DimapProductConstants.TAG_DATASET_DESCRIPTION);
                }
            }
            if (description != null && description.length() > 0) {
                product.setDescription(description);
            }
        }

        private void setSceneRasterStartAndStopTime(final Product product) {
            final ProductData.UTC sceneRasterStartTime = getSceneRasterStartTime();
            if (sceneRasterStartTime != null) {
                product.setStartTime(sceneRasterStartTime);
            }
            final ProductData.UTC sceneRasterStopTime = getSceneRasterStopTime();
            if (sceneRasterStopTime != null) {
                product.setEndTime(sceneRasterStopTime);
            }
        }

        private String getProductType() {
            final Element child = getRootElement().getChild(DimapProductConstants.TAG_PRODUCTION);
            return child.getChildTextTrim(DimapProductConstants.TAG_PRODUCT_TYPE);
        }

        private ProductData.UTC getSceneRasterStartTime() {
            final Element child = getRootElement().getChild(DimapProductConstants.TAG_PRODUCTION);
            String timeString = child.getChildTextTrim(DimapProductConstants.TAG_PRODUCT_SCENE_RASTER_START_TIME);
            if (StringUtils.isNullOrEmpty(timeString)) {
                timeString = child.getChildTextTrim(DimapProductConstants.TAG_OLD_SCENE_RASTER_START_TIME);
            }
            return parseTimeString(timeString);
        }

        private ProductData.UTC getSceneRasterStopTime() {
            final Element child = getRootElement().getChild(DimapProductConstants.TAG_PRODUCTION);
            String timeString = child.getChildTextTrim(DimapProductConstants.TAG_PRODUCT_SCENE_RASTER_STOP_TIME);
            if (StringUtils.isNullOrEmpty(timeString)) {
                timeString = child.getChildTextTrim(DimapProductConstants.TAG_OLD_SCENE_RASTER_STOP_TIME);
            }
            return parseTimeString(timeString);
        }

        private static ProductData.UTC parseTimeString(final String timeString) {
            if (timeString != null && timeString.trim().length() > 0) {
                ProductData.UTC utc = null;
                try {
                    utc = ProductData.UTC.parse(timeString);
                } catch (ParseException e) {
                    Debug.trace(e);
                }
                return utc;
            } else {
                return null;
            }
        }

        private int getSceneRasterWidth() {
            final Element child = getRootElement().getChild(DimapProductConstants.TAG_RASTER_DIMENSIONS);
            return Integer.parseInt(child.getChildTextTrim(DimapProductConstants.TAG_NCOLS));
        }

        private int getSceneRasterHeight() {
            final Element child = getRootElement().getChild(DimapProductConstants.TAG_RASTER_DIMENSIONS);
            return Integer.parseInt(child.getChildTextTrim(DimapProductConstants.TAG_NROWS));
        }

        private Element getRootElement() {
            return getDom().getRootElement();
        }
    }

}
