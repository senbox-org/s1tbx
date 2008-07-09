/*
 * $Id$
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
package org.esa.beam.dataio.obpg.hdf;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.bandreader.ObpgBandReader;
import org.esa.beam.dataio.obpg.bandreader.ObpgBandReaderFactory;
import org.esa.beam.dataio.obpg.ObpgProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObpgUtils {

    static final String KEY_NAME = "Product Name";
    static final String KEY_TYPE = "Title";
    static final String KEY_WIDTH = "Pixels per Scan Line";
    static final String KEY_HEIGHT = "Number of Scan Lines";
    static final String KEY_START_NODE = "Start Node";
    static final String KEY_END_NODE = "End Node";
    static final String SENSOR_BAND_PARAMETERS = "Sensor_Band_Parameters";
    static final String SCAN_LINE_ATTRIBUTES = "Scan_Line_Attributes";

    HdfFacade hdf = new HdfFacade();

    /**
     * Converts a <code>HdfAttribute</code> to a <code>MetadataAttribute</code>
     *
     * @param hdfAttribute the hdf attribute to convert
     * @return the product metadata attribute
     */
    public MetadataAttribute attributeToMetadata(final HdfAttribute hdfAttribute) {
        ProductData prodData = null;
        MetadataAttribute attrib = null;

        switch (hdfAttribute.getHdfType()) {
            case HDFConstants.DFNT_CHAR:
            case HDFConstants.DFNT_UCHAR8:
                prodData = ProductData.createInstance(hdfAttribute.getStringValue());
                break;

            case HDFConstants.DFNT_UINT8:
            case HDFConstants.DFNT_INT8:
            case HDFConstants.DFNT_UINT16:
            case HDFConstants.DFNT_INT16:
            case HDFConstants.DFNT_INT32:
            case HDFConstants.DFNT_UINT32:
                prodData = ProductData.createInstance(hdfAttribute.getIntValues());
                break;

            case HDFConstants.DFNT_FLOAT32:
                prodData = ProductData.createInstance(hdfAttribute.getFloatValues());
                break;

            case HDFConstants.DFNT_DOUBLE:
                prodData = ProductData.createInstance(hdfAttribute.getDoubleValues());
                break;
        }

        if (prodData != null) {
            attrib = new MetadataAttribute(hdfAttribute.getName(), prodData, true);
        }
        return attrib;
    }

    /**
     * Decodes the hdf data type into a product data type.
     *
     * @param hdfType
     * @return product data type
     * @see ProductData
     */
    public int decodeHdfDataType(final int hdfType) {
        switch (hdfType) {
            case HDFConstants.DFNT_UCHAR8:
            case HDFConstants.DFNT_UINT8:
                return ProductData.TYPE_UINT8;

            case HDFConstants.DFNT_CHAR8:
            case HDFConstants.DFNT_INT8:
                return ProductData.TYPE_INT8;

            case HDFConstants.DFNT_INT16:
                return ProductData.TYPE_INT16;

            case HDFConstants.DFNT_UINT16:
                return ProductData.TYPE_UINT16;

            case HDFConstants.DFNT_INT32:
                return ProductData.TYPE_INT32;

            case HDFConstants.DFNT_UINT32:
                return ProductData.TYPE_UINT32;

            case HDFConstants.DFNT_FLOAT32:
                return ProductData.TYPE_FLOAT32;

            case HDFConstants.DFNT_FLOAT64:
                return ProductData.TYPE_FLOAT64;

            default:
                return ProductData.TYPE_UNDEFINED;
        }
    }

    /**
     * Reads the global attributes from the hdf file passed in.
     *
     * @param sdStart the HD interface identifier of the file
     * @return an instance of HdfGlobalAttributes which contains all the
     *         global hdf attributes read from file.
     * @throws ncsa.hdf.hdflib.HDFException -
     */
    public List<HdfAttribute> readGlobalAttributes(final int sdStart) throws HDFException {
        Debug.trace("reading global attributes ...");

        // request number of datasets (fileInfo[0]) and number of global attributes (fileInfo[1])
        final SDFileInfo fileInfo1 = hdf.getSDFileInfo(sdStart);
        if (fileInfo1 != null) {
            return hdf.readAttributes(sdStart, fileInfo1.attributeCount);
        } else {
            final String message = "Unable to read global metadata.";
            Debug.trace("... " + message);
            throw new HDFException(message);
        }
    }

    public static File getInputFile(final Object o) {
        final File inputFile;
        if (o instanceof File) {
            inputFile = (File) o;
        } else if (o instanceof String) {
            inputFile = new File((String) o);
        } else {
            throw new IllegalArgumentException("unsupported input source: " + o);
        }
        return inputFile;
    }

    public Product createProductBody(final List<HdfAttribute> globalAttributes) throws ProductIOException {
        String productName = null;
        String productType = null;
        Integer sceneRasterWidth = null;
        Integer sceneRasterHeight = null;
        for (HdfAttribute attribute : globalAttributes) {
            final String name = attribute.getName();
            if (KEY_NAME.equalsIgnoreCase(name)) {
                productName = attribute.getStringValue().trim();
            } else if (KEY_TYPE.equalsIgnoreCase(name)) {
                productType = "OBPG " + attribute.getStringValue().trim();
            } else if (KEY_WIDTH.equalsIgnoreCase(name) && attribute.getElemCount() == 1) {
                sceneRasterWidth = attribute.getIntValues()[0];
            } else if (KEY_HEIGHT.equalsIgnoreCase(name) && attribute.getElemCount() == 1) {
                sceneRasterHeight = attribute.getIntValues()[0];
            }
        }
        if (productName == null) {
            throw new ProductIOException("Global attribute '" + KEY_NAME + "' is missing.");
        } else if (productType == null) {
            throw new ProductIOException("Global attribute '" + KEY_TYPE + "' is missing.");
        } else if (sceneRasterWidth == null) {
            throw new ProductIOException("Global attribute '" + KEY_WIDTH + "' is missing.");
        } else if (sceneRasterHeight == null) {
            throw new ProductIOException("Global attribute '" + KEY_HEIGHT + "' is missing.");
        }
        final Product product = new Product(productName, productType, sceneRasterWidth, sceneRasterHeight);
        product.setDescription(productName);
        return product;
    }

    public boolean mustFlip(final List<HdfAttribute> globalAttributes) throws ProductIOException {
        Boolean startNodeAscending = null;
        Boolean endNodeAscending = null;
        for (HdfAttribute attribute : globalAttributes) {
            final String name = attribute.getName();
            if (KEY_START_NODE.equalsIgnoreCase(name)) {
                startNodeAscending = "Ascending".equalsIgnoreCase(attribute.getStringValue().trim());
            } else if (KEY_END_NODE.equalsIgnoreCase(name)) {
                endNodeAscending = "Ascending".equalsIgnoreCase(attribute.getStringValue().trim());
            }
        }
        return (startNodeAscending != null && endNodeAscending != null) && (startNodeAscending && endNodeAscending);
    }

    public void addGlobalMetadata(final Product product, final List<HdfAttribute> globalAttributes) {
        final MetadataElement globalElement = new MetadataElement("Global_Attributes");
        addAttributesToElement(globalAttributes, globalElement);

        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addElement(globalElement);
    }

    public SdsInfo[] extractSdsData(final int sdStart) throws HDFException {
        final SDFileInfo sdFileInfo = hdf.getSDFileInfo(sdStart);
        final ArrayList<SdsInfo> sdsInfoList = new ArrayList<SdsInfo>();
        for (int i = 0; i < sdFileInfo.sdsCount; i++) {
            sdsInfoList.add(hdf.getSdsInfo(sdStart, i));
        }

        return sdsInfoList.toArray(new SdsInfo[sdsInfoList.size()]);
    }

    public int openHdfFileReadOnly(final String path) throws HDFException {
        return hdf.openHdfFileReadOnly(path);
    }

    public int openSdInterfaceReadOnly(final String path) throws HDFException {
        return hdf.openSdInterfaceReadOnly(path);
    }

    public boolean closeHdfFile(final int fileId) throws HDFException {
        return hdf.closeHdfFile(fileId);
    }

    public boolean isHdfFile(final String path) throws HDFException {
        return hdf.isHdfFile(path);
    }

    public SdsInfo getSdsInfo(final int sdsId) throws HDFException {
        return hdf.getSdsInfo(sdsId);
    }

    public void addScientificMetadata(final Product product, final SdsInfo[] sdsInfos) throws HDFException {
        final int numLines = product.getSceneRasterHeight();
        final MetadataElement sensorBandParam = getMetadataElementSave(product, SENSOR_BAND_PARAMETERS);
        final MetadataElement scanLineAttrib = getMetadataElementSave(product, SCAN_LINE_ATTRIBUTES);
        for (SdsInfo sdsInfo : sdsInfos) {
            if (sdsInfo.getNumDimensions() == 1) {
                final String name = sdsInfo.getName();

                final int dataType = decodeHdfDataType(sdsInfo.getHdfDataType());
                final int size = sdsInfo.getDimensions()[0];
                final ProductData data = ProductData.createInstance(dataType, size);
                hdf.readProductData(sdsInfo, data);
                final MetadataAttribute attribute = new MetadataAttribute("data", data, true);

                final MetadataElement sdsElement = new MetadataElement(name);
                sdsElement.addAttribute(attribute);
                if (size == numLines) {
                    scanLineAttrib.addElement(sdsElement);
                } else {
                    sensorBandParam.addElement(sdsElement);
                }

                final List<HdfAttribute> list = hdf.readAttributes(sdsInfo.getSdsID(), sdsInfo.getNumAttributes());
                for (HdfAttribute hdfAttribute : list) {
                    final String attribName = hdfAttribute.getName();
                    if ("units".equals(attribName)) {
                        attribute.setUnit(hdfAttribute.getStringValue());
                    } else if ("long_name".equals(attribName)) {
                        attribute.setDescription(hdfAttribute.getStringValue());
                    } else {
                        addAttributeToElement(sdsElement, hdfAttribute);
                    }
                }
            }
        }
    }

    private MetadataElement getMetadataElementSave(Product product, String name) {
        final MetadataElement metadataElement = product.getMetadataRoot().getElement(name);
        final MetadataElement namedElem;
        if (metadataElement == null) {
            namedElem = new MetadataElement(name);
            product.getMetadataRoot().addElement(namedElem);
        } else {
            namedElem = metadataElement;
        }
        return namedElem;
    }

    public Map<Band, ObpgBandReader> addBands(final Product product,
                                              final SdsInfo[] sdsInfos,
                                              HashMap<String, String> l2BandInfoMap,
                                              HashMap<String, String> l2FlagsInfoMap) throws HDFException {
        final HashMap<Band, ObpgBandReader> readerMap = new HashMap<Band, ObpgBandReader>();
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        int spectralBandIndex = 0;
        for (SdsInfo sdsInfo : sdsInfos) {
            if (sdsInfo.getNumDimensions() == 2) {
                final int[] dimensions = sdsInfo.getDimensions();
                final int height = dimensions[0];
                final int width = dimensions[1];
                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    final String name = sdsInfo.getName();
                    final int dataType = decodeHdfDataType(sdsInfo.getHdfDataType());
                    final Band band = new Band(name, dataType, width, height);
                    final String validExpression = l2BandInfoMap.get(name);
                    if (validExpression != null && !validExpression.equals("")) {
                        band.setValidPixelExpression(validExpression);
                    }
                    product.addBand(band);
                    if (name.matches("nLw_\\d{3,}")) {
                        final float wavelength = Float.parseFloat(name.substring(4));
                        band.setSpectralWavelength(wavelength);
                        band.setSpectralBandIndex(spectralBandIndex++);
                    }

                    final ObpgBandReader[] bandReaders = ObpgBandReaderFactory.getReaders(sdsInfo, dataType);
                    readerMap.put(band, bandReaders[0]);
                    final List<HdfAttribute> list = hdf.readAttributes(sdsInfo.getSdsID(), sdsInfo.getNumAttributes());
                    FlagCoding flagCoding = null;
                    for (HdfAttribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getName();
                        if ("units".equals(attribName)) {
                            band.setUnit(hdfAttribute.getStringValue());
                        } else if ("long_name".equals(attribName)) {
                            band.setDescription(hdfAttribute.getStringValue());
                        } else if ("slope".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getDoubleValues()[0]);
                        } else if ("intercept".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getDoubleValues()[0]);
                        } else if (attribName.matches("f\\d\\d_name")) {
                            if (flagCoding == null) {
                                flagCoding = new FlagCoding(name);
                            }
                            final String flagName = hdfAttribute.getStringValue();
                            final int flagMask = convertToFlagMask(attribName);
                            flagCoding.addFlag(flagName, flagMask, l2FlagsInfoMap.get(flagName));
                        }
                    }
                    if (flagCoding != null) {
                        band.setSampleCoding(flagCoding);
                        product.getFlagCodingGroup().add(flagCoding);
                    }
                }
            }
        }
        return readerMap;
    }

    public void addGeocoding(final Product product, final SdsInfo[] sdsInfos, boolean mustFlip) throws HDFException, IOException {
        SdsInfo longitudeSds = null;
        SdsInfo latitudeSds = null;
        for (SdsInfo sdsInfo : sdsInfos) {
            final String name = sdsInfo.getName();
            if ("longitude".equalsIgnoreCase(name)) {
                longitudeSds = sdsInfo;
            } else if ("latitude".equalsIgnoreCase(name)) {
                latitudeSds = sdsInfo;
            }
        }
        if (latitudeSds != null && longitudeSds != null) {
            final ProductData lonRawData = readData(longitudeSds);
            final ProductData latRawData = readData(latitudeSds);
            final Band latBand = product.addBand(latitudeSds.getName(), ProductData.TYPE_FLOAT32);
            final Band lonBand = product.addBand(longitudeSds.getName(), ProductData.TYPE_FLOAT32);

            final MetadataElement scientificElement = product.getMetadataRoot().getElement(SENSOR_BAND_PARAMETERS);
            final MetadataElement cntlPointElem = scientificElement.getElement("cntl_pt_cols");
            final MetadataAttribute attribute = cntlPointElem.getAttribute("data");
            final int[] colPoints = (int[]) attribute.getDataElems();

            computeLatLonBandData(latBand, lonBand, latRawData, lonRawData, colPoints, mustFlip);

            product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5, ProgressMonitor.NULL));
        }
    }

    public void addBitmaskDefinitions(Product product, BitmaskDef[] defaultBitmaskDefs) {
        for (BitmaskDef defaultBitmaskDef : defaultBitmaskDefs) {
            product.addBitmaskDef(defaultBitmaskDef);
        }
    }

    private void computeLatLonBandData(final Band latBand, final Band lonBand,
                                       final ProductData latRawData, final ProductData lonRawData,
                                       final int[] colPoints, boolean mustFlip) {
        latBand.ensureRasterData();
        lonBand.ensureRasterData();

        final float[] latRawFloats = (float[]) latRawData.getElems();
        final float[] lonRawFloats = (float[]) lonRawData.getElems();
        final float[] latFloats = (float[]) latBand.getDataElems();
        final float[] lonFloats = (float[]) lonBand.getDataElems();
        final int rawWidth = colPoints.length;
        final int width = latBand.getRasterWidth();
        final int height = latBand.getRasterHeight();

        int colPointIdx = 0;
        int p1 = colPoints[colPointIdx] - 1;
        int p2 = colPoints[++colPointIdx] - 1;
        for (int x = 0; x < width; x++) {
            if (x == p2 && colPointIdx < rawWidth - 1) {
                p1 = p2;
                p2 = colPoints[++colPointIdx] - 1;
            }
            final int steps = p2 - p1;
            final double step = 1.0 / steps;
            final double weight = step * (x - p1);
            for (int y = 0; y < height; y++) {
                final int rawPos2 = y * rawWidth + colPointIdx;
                final int rawPos1 = rawPos2 - 1;
                final int pos = y * width + x;
                latFloats[pos] = computePixel(latRawFloats[rawPos1], latRawFloats[rawPos2], weight);
                lonFloats[pos] = computePixel(lonRawFloats[rawPos1], lonRawFloats[rawPos2], weight);
            }
        }

        if (mustFlip) {
            ObpgProductReader.reverse(latFloats);
            ObpgProductReader.reverse(lonFloats);
        }

        latBand.setSynthetic(true);
        lonBand.setSynthetic(true);
    }

    private float computePixel(final float a, final float b, final double weight) {
        if ((b - a) > 180) {
            final float b2 = b - 360;
            final double v = a + (b2 - a) * weight;
            if (v >= -180) {
                return (float) v;
            } else {
                return (float) (v + 360);
            }
        } else {
            return (float) (a + (b - a) * weight);
        }
    }

    private ProductData readData(SdsInfo sdsInfo) throws HDFException {
        final int[] longDims = sdsInfo.getDimensions();
        final int numElems = longDims[0] * longDims[1];
        final int dataType = decodeHdfDataType(sdsInfo.getHdfDataType());
        final ProductData data = ProductData.createInstance(dataType, numElems);
        return hdf.readProductData(sdsInfo, data);
    }

    int convertToFlagMask(String name) {
        if (name.matches("f\\d\\d_name")) {
            final String number = name.substring(1, 3);
            final int i = Integer.parseInt(number) - 1;
            if (i >= 0) {
                return 1 << i;
            }
        }
        return 0;
    }

    private void addAttributesToElement(final List<HdfAttribute> attributes, final MetadataElement element) {
        for (HdfAttribute hdfAttribute : attributes) {
            addAttributeToElement(element, hdfAttribute);
        }
    }

    private void addAttributeToElement(final MetadataElement element, final HdfAttribute hdfAttribute) {
        final MetadataAttribute metadataAttribute = attributeToMetadata(hdfAttribute);
        element.addAttribute(metadataAttribute);
    }
}
