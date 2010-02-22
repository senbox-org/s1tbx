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
package org.esa.beam.dataio.obpg;

import org.esa.beam.dataio.obpg.ObpgProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bc.ceres.core.ProgressMonitor;

public class ObpgUtils {

    static final String KEY_NAME = "Product Name";
    static final String KEY_TYPE = "Title";
    static final String KEY_WIDTH = "Pixels per Scan Line";
    static final String KEY_HEIGHT = "Number of Scan Lines";
    static final String KEY_START_NODE = "Start Node";
    static final String KEY_END_NODE = "End Node";
    static final String KEY_START_TIME = "Start Time";
    static final String KEY_END_TIME = "End Time";
    static final String SENSOR_BAND_PARAMETERS = "Sensor_Band_Parameters";
    static final String SENSOR_BAND_PARAMETERS_GROUP = "Sensor Band Parameters";
    static final String SCAN_LINE_ATTRIBUTES = "Scan_Line_Attributes";
    static final String SCAN_LINE_ATTRIBUTES_GROUP = "Scan-Line Attributes";

    MetadataAttribute attributeToMetadata(Attribute attribute) {
        final int productDataType = getProductDataType(attribute.getDataType(), false, false);
        if (productDataType != -1) {
            ProductData productData;
            if (attribute.isString()) {
                productData = ProductData.createInstance(attribute.getStringValue());
            } else if (attribute.isArray()) {
                productData = ProductData.createInstance(productDataType, attribute.getLength());
                productData.setElems(attribute.getValues().getStorage());
            } else {
                productData = ProductData.createInstance(productDataType, 1);
                productData.setElems(attribute.getValues().getStorage());
            }
            return new MetadataAttribute(attribute.getName(), productData, true);
        }
        return null;
    }
    
    public static int getProductDataType(Variable variable) {
        return getProductDataType(variable.getDataType(), variable.isUnsigned(), true);
    }
    
    public static int getProductDataType(DataType dataType, boolean unsigned, boolean rasterDataOnly) {
        if (dataType == DataType.BYTE) {
            return unsigned ? ProductData.TYPE_UINT8 : ProductData.TYPE_INT8;
        } else if (dataType == DataType.SHORT) {
            return unsigned ? ProductData.TYPE_UINT16 : ProductData.TYPE_INT16;
        } else if (dataType == DataType.INT) {
            return unsigned ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        } else if (dataType == DataType.FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else if (dataType == DataType.DOUBLE) {
            return ProductData.TYPE_FLOAT64;
        } else if (!rasterDataOnly) {
            if (dataType == DataType.CHAR) {
                // return ProductData.TYPE_ASCII; todo - handle this case
            } else if (dataType == DataType.STRING) {
                return ProductData.TYPE_ASCII;
            }
        }
        return -1;
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

    public Product createProductBody(List<Attribute> gloabAttributes) throws ProductIOException {
        String productName = getStringAttribute(gloabAttributes, KEY_NAME);
        String productType = "OBPG " + getStringAttribute(gloabAttributes, KEY_TYPE);
        int sceneRasterWidth = getIntAttribute(gloabAttributes, KEY_WIDTH);
        int sceneRasterHeight = getIntAttribute(gloabAttributes, KEY_HEIGHT);

        final Product product = new Product(productName, productType, sceneRasterWidth, sceneRasterHeight);
        product.setDescription(productName);
        String startTime = getStringAttribute(gloabAttributes, KEY_START_TIME);
        String endTime = getStringAttribute(gloabAttributes, KEY_END_TIME);
        
        final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyDDDHHmmssSSS");
        try {
            final Date startDate = dateFormat.parse(startTime);
            String millis = startTime.substring(startTime.length()-3);
            final ProductData.UTC utcStart = ProductData.UTC.create(startDate, Long.parseLong(millis)*1000);
            product.setStartTime(utcStart);
            
            final Date endDate = dateFormat.parse(endTime);
            millis = endTime.substring(endTime.length()-3);
            final ProductData.UTC utcEnd = ProductData.UTC.create(endDate, Long.parseLong(millis)*1000);
            product.setEndTime(utcEnd);
        } catch (ParseException ignored) {
        }

        
        return product;
    }
    
    private String getStringAttribute(List<Attribute> gloabAttributes, String key) throws ProductIOException {
        Attribute attribute = findAttribute(key, gloabAttributes);
        if (attribute == null || attribute.getLength() != 1) {
            throw new ProductIOException("Global attribute '" + key + "' is missing.");
        } else {
            return attribute.getStringValue().trim();
        }
    }
    
    private int getIntAttribute(List<Attribute> gloabAttributes, String key) throws ProductIOException {
        Attribute attribute = findAttribute(key, gloabAttributes);
        if (attribute == null) {
            throw new ProductIOException("Global attribute '" + key + "' is missing.");
        } else {
            return attribute.getNumericValue(0).intValue();
        }
    }
    
    private Attribute findAttribute(String name, List<Attribute> attributesList) {
        for (Attribute a : attributesList) {
            if (name.equals(a.getName()))
            return a;
        }
        return null;
    }

    public boolean mustFlip(final NetcdfFile ncfile) throws ProductIOException {
        Attribute startAttr = ncfile.findGlobalAttributeIgnoreCase(KEY_START_NODE);
        boolean startNodeAscending = false;
        if (startAttr != null) {
            startNodeAscending = "Ascending".equalsIgnoreCase(startAttr.getStringValue().trim());
        }
        Attribute endAttr = ncfile.findGlobalAttributeIgnoreCase(KEY_END_NODE);
        boolean endNodeAscending = false;
        if (endAttr != null) {
            endNodeAscending = "Ascending".equalsIgnoreCase(endAttr.getStringValue().trim());
        }
        
        return (startNodeAscending && endNodeAscending);
    }

    public void addGlobalMetadata(final Product product, List<Attribute> globalAttributes) {
        final MetadataElement globalElement = new MetadataElement("Global_Attributes");
        addAttributesToElement(globalAttributes, globalElement);

        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addElement(globalElement);
    }

    public void addScientificMetadata(Product product, NetcdfFile ncFile) throws IOException {
        final MetadataElement scanLineAttrib = getMetadataElementSave(product, SCAN_LINE_ATTRIBUTES);
        Group group = ncFile.findGroup(SCAN_LINE_ATTRIBUTES_GROUP);
        handleMetadataGroup(group, scanLineAttrib);
        
        final MetadataElement sensorBandParam = getMetadataElementSave(product, SENSOR_BAND_PARAMETERS);
        group = ncFile.findGroup(SENSOR_BAND_PARAMETERS_GROUP);
        handleMetadataGroup(group, sensorBandParam);
    }
    
    private void handleMetadataGroup(Group group, MetadataElement metadataElement) throws IOException {
        List<Variable> variables = group.getVariables();
        for (Variable variable : variables) {
            final String name = variable.getShortName();
            final int dataType = getProductDataType(variable);
            Array array = variable.read();
            final ProductData data = ProductData.createInstance(dataType, array.getStorage());
            final MetadataAttribute attribute = new MetadataAttribute("data", data, true);
            
            final MetadataElement sdsElement = new MetadataElement(name);
            sdsElement.addAttribute(attribute);
            metadataElement.addElement(sdsElement);
            
            final List<Attribute> list = variable.getAttributes();
            for (Attribute hdfAttribute : list) {
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

    public Map<Band, Variable> addBands(final Product product,
                                              final NetcdfFile ncFile,
                                              HashMap<String, String> l2BandInfoMap,
                                              HashMap<String, String> l2FlagsInfoMap) {
        final HashMap<Band, Variable> readerMap = new HashMap<Band, Variable>();
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        int spectralBandIndex = 0;
        List<Variable> variables = ncFile.getVariables();
        for (Variable variable : variables) {
            if (variable.getRank() == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0];
                final int width = dimensions[1];
                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    final String name = variable.getShortName();
                    final int dataType = getProductDataType(variable);
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

                    readerMap.put(band, variable);
                    final List<Attribute> list = variable.getAttributes();
                    FlagCoding flagCoding = null;
                    for (Attribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getName();
                        if ("units".equals(attribName)) {
                            band.setUnit(hdfAttribute.getStringValue());
                        } else if ("long_name".equals(attribName)) {
                            band.setDescription(hdfAttribute.getStringValue());
                        } else if ("slope".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("intercept".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
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

    public void addGeocoding(final Product product, NetcdfFile ncfile, boolean mustFlip) throws IOException {
        final String navGroup = "Navigation Data";
        final String longitude = "longitude";
        final String latitude = "latitude";
        String cntlPoints = "cntl_pt_cols";
        Band latBand = null;
        Band lonBand = null;
        if (product.containsBand(latitude) && product.containsBand(longitude)) {
            latBand = product.getBand(latitude);
            lonBand = product.getBand(longitude);
        } else {
            Variable latVar = ncfile.findVariable(navGroup + "/" + latitude);
            Variable lonVar = ncfile.findVariable(navGroup + "/" + longitude);
            Variable cntlPointVar = ncfile.findVariable(navGroup + "/" + cntlPoints);
            if (latVar != null && lonVar != null && cntlPoints != null) {
                final ProductData lonRawData = readData(lonVar);
                final ProductData latRawData = readData(latVar);
                latBand = product.addBand(latVar.getShortName(), ProductData.TYPE_FLOAT32);
                lonBand = product.addBand(lonVar.getShortName(), ProductData.TYPE_FLOAT32);

                Array cntArray = cntlPointVar.read();
                int[] colPoints = (int[]) cntArray.getStorage();
                computeLatLonBandData(latBand, lonBand, latRawData, lonRawData, colPoints, mustFlip);
            }
        }
        if (latBand != null && lonBand != null) {
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

    private ProductData readData(Variable variable) throws IOException {
        final int dataType = getProductDataType(variable);
        Array array = variable.read();
        return ProductData.createInstance(dataType, array.getStorage());
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

    private void addAttributesToElement(List<Attribute> globalAttributes, final MetadataElement element) {
        for (Attribute attribute : globalAttributes) {
            addAttributeToElement(element, attribute);
        }
    }

    private void addAttributeToElement(final MetadataElement element, final Attribute attribute) {
        final MetadataAttribute metadataAttribute = attributeToMetadata(attribute);
        element.addAttribute(metadataAttribute);
    }
}
