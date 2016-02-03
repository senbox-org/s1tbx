/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.csv.dataio.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelTimeCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.csv.dataio.CsvFile;
import org.esa.snap.csv.dataio.CsvSource;
import org.esa.snap.csv.dataio.CsvSourceParser;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.stream.DoubleStream;

import static org.esa.snap.csv.dataio.Constants.*;

/**
 * The CsvProductReader is able to read a CSV file as a product.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductReader extends AbstractProductReader {

    private CsvSourceParser parser;
    private CsvSource source;
    private Product product;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be {@code null} for internal reader
     *                     implementations
     */
    protected CsvProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (parser != null) {
            parser.close();
        }
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = getInputFile();
        parser = CsvFile.createCsvSourceParser(inputFile.getAbsolutePath());
        source = parser.parseMetadata();
        String sceneRasterWidthProperty = source.getProperties().get(PROPERTY_NAME_SCENE_RASTER_WIDTH);
        final int sceneRasterWidth;
        final int sceneRasterHeight;
        int recordCount = source.getRecordCount();
        if (sceneRasterWidthProperty != null) {
            sceneRasterWidth = Integer.parseInt(sceneRasterWidthProperty);
            sceneRasterHeight = recordCount % sceneRasterWidth == 0 ? recordCount / sceneRasterWidth :
                                recordCount / sceneRasterWidth + 1;
        } else {
            if (isSquareNumber(recordCount)) {
                sceneRasterWidth = (int) Math.sqrt(recordCount);
            } else {
                sceneRasterWidth = (int) Math.sqrt(recordCount) + 1;
            }
            //noinspection SuspiciousNameCombination
            sceneRasterHeight = sceneRasterWidth;
        }

        // todo - get name and type from properties, if existing

        String productName = StringUtils.createValidName(FileUtils.getFilenameWithoutExtension(inputFile), null, '_');
        product = new Product(productName, "CSV", sceneRasterWidth, sceneRasterHeight);
        product.setPreferredTileSize(sceneRasterWidth, sceneRasterHeight);
        product.setFileLocation(inputFile);
        product.setProductReader(this);
        initTimeCoding();
        initBands();
        initGeocoding();

        // todo - somehow handle attributes which are of no band type, such as utc
        //          timeCoding
        // todo - put properties into metadata
        //          partly done
        // todo - separation of bands and tiepoint grids?!
        return product;
    }

    private void initGeocoding() {
        final Band latBand = fetchBand(LAT_NAMES);
        if (latBand == null) {
            SystemUtils.LOG.info("Latitude information not available.");
            SystemUtils.LOG.warning("Unable to initialize PixelGeoCoding.");
            return;
        }
        final Band lonBand = fetchBand(LON_NAMES);
        if (lonBand == null) {
            SystemUtils.LOG.info("Longitude information not available.");
            SystemUtils.LOG.warning("Unable to initialize PixelGeoCoding.");
            return;
        }
        final String validMask = latBand.getValidMaskExpression();
        final int searchRadius = 5;
        GeoCoding gc = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, validMask, searchRadius);
//        GeoCoding gc = new PixelGeoCoding(latBand, lonBand, validMask, searchRadius);
        product.setSceneGeoCoding(gc);
    }

    private Band fetchBand(String[] names) {
        for (String name : names) {
            if (product.containsBand(name)) {
                return product.getBand(name);
            }
        }
        return null;
    }

    private void initTimeCoding() throws IOException {
        final String timeColumnName = source.getProperties().get(PROPERTY_NAME_TIME_COLUMN);
        if (StringUtils.isNotNullAndNotEmpty(timeColumnName)) {
            addCsvHeaderProperty(PROPERTY_NAME_TIME_COLUMN, timeColumnName);
        }
        final String timePattern = source.getProperties().get(PROPERTY_NAME_TIME_PATTERN);
        if (StringUtils.isNotNullAndNotEmpty(timePattern)) {
            addCsvHeaderProperty(PROPERTY_NAME_TIME_PATTERN, timePattern);
        }
        for (AttributeDescriptor descriptor : source.getFeatureType().getAttributeDescriptors()) {
            final String colName = descriptor.getName().toString();
            if (timeColumnName != null && !timeColumnName.equals(colName)) {
                continue;
            }
            if (isUTC(descriptor)) {
                final double[] timeMJD = getTimeMJD(colName);
                if (timeMJD != null) {
                    final int width = product.getSceneRasterWidth();
                    final int height = product.getSceneRasterHeight();
                    product.setSceneTimeCoding(new CSVTimeCoding(timeMJD, width, height, colName));
                    initStartEndTime(timeMJD);
                    return;
                }
            }
        }
        SystemUtils.LOG.warning("Not able to create PixelTimeCoding for product '" + product.getFileLocation().getAbsolutePath() + "'");
    }

    private void addCsvHeaderProperty(String name, String stringValue) {
        final MetadataElement headerElement = getCsvMetadataHeader();
        headerElement.addAttribute(new MetadataAttribute(name, ProductData.createInstance(stringValue), true));
    }

    private MetadataElement getCsvMetadataHeader() {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement headerElement;
        if (metadataRoot.containsElement(NAME_METADATA_ELEMENT_CSV_HEADER_PROPERTIES)) {
            headerElement = metadataRoot.getElement(NAME_METADATA_ELEMENT_CSV_HEADER_PROPERTIES);
        } else {
            headerElement = new MetadataElement(NAME_METADATA_ELEMENT_CSV_HEADER_PROPERTIES);
            metadataRoot.addElement(headerElement);
        }
        return headerElement;
    }

    private void initStartEndTime(double[] timeMJD) {
        product.setStartTime(new ProductData.UTC(DoubleStream.of(timeMJD).min().getAsDouble()));
        product.setEndTime(new ProductData.UTC(DoubleStream.of(timeMJD).max().getAsDouble()));
    }

    private boolean isUTC(AttributeDescriptor descriptor) {
        final Class<?> binding = descriptor.getType().getBinding();
        return binding.getSimpleName().toLowerCase().equals("utc");
    }

    private double[] getTimeMJD(String colName) throws IOException {
        Object[] objects = parser.parseRecords(0, source.getRecordCount(), colName);
        double[] timeMJD = new double[objects.length];
        for (int i = 0; i < objects.length; i++) {
            ProductData.UTC date = (ProductData.UTC) objects[i];
            if (date == null) {
                return null;
            }
            timeMJD[i] = date.getMJD();
        }
        return timeMJD;
    }

    private void initBands() {
        for (AttributeDescriptor descriptor : source.getFeatureType().getAttributeDescriptors()) {
            Class<?> binding = descriptor.getType().getBinding();
            if (isAccessibleBandType(binding)) {
                int type = getProductDataType(binding);
                product.addBand(descriptor.getName().toString(), type);
            }
        }
    }

    private File getInputFile() throws FileNotFoundException {
        final File inputFile = new File(getInput().toString());
        if (!inputFile.exists()) {
            throw new FileNotFoundException(inputFile.getPath());
        }

        return inputFile;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        SystemUtils.LOG.log(Level.FINEST, MessageFormat.format(
                    "reading band data (" + destBand.getName() + ") from {0} to {1}",
                    destOffsetY * destWidth, sourceOffsetY * destWidth + destWidth * destHeight));
        pm.beginTask("reading band data...", destWidth * destHeight);

        Object[] values;
        synchronized (parser) {
            values = parser.parseRecords(destOffsetY * destBand.getRasterWidth() + destOffsetX, destWidth * destHeight, destBand.getName());
        }
        getProductData(values, destBuffer);
        pm.done();
    }

    void getProductData(Object[] elems, ProductData destBuffer) {
        switch (destBuffer.getType()) {
            case ProductData.TYPE_FLOAT32: {
                for (int i = 0; i < destBuffer.getNumElems(); i++) {
                    final Object elem;
                    if (i < elems.length) {
                        elem = elems[i] != null ? elems[i] : Float.NaN;
                    } else {
                        elem = Float.NaN;
                    }
                    destBuffer.setElemFloatAt(i, (Float) elem);
                }
                break;
            }
            case ProductData.TYPE_FLOAT64: {
                for (int i = 0; i < destBuffer.getNumElems(); i++) {
                    final Object elem;
                    if (i < elems.length) {
                        elem = elems[i] != null ? elems[i] : Double.NaN;
                    } else {
                        elem = Double.NaN;
                    }
                    destBuffer.setElemDoubleAt(i, (Double) elem);
                }
                break;
            }
            case ProductData.TYPE_INT8: {
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i] != null ? elems[i] : 0;
                    destBuffer.setElemIntAt(i, (Byte) elem);
                }
                break;
            }
            case ProductData.TYPE_INT16: {
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i] != null ? elems[i] : 0;
                    destBuffer.setElemIntAt(i, (Short) elem);
                }
                break;
            }
            case ProductData.TYPE_INT32: {
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i] != null ? elems[i] : 0;
                    destBuffer.setElemIntAt(i, (Integer) elem);
                }
                break;
            }
            default: {
                throw new IllegalArgumentException(
                            "Unsupported type '" + ProductData.getTypeString(destBuffer.getType()) + "'.");
            }
        }
    }

    int getProductDataType(Class<?> type) {
        if (type.getSimpleName().toLowerCase().equals("string")) {
            return ProductData.TYPE_ASCII;
        } else if (type.getSimpleName().toLowerCase().equals("float")) {
            return ProductData.TYPE_FLOAT32;
        } else if (type.getSimpleName().toLowerCase().equals("double")) {
            return ProductData.TYPE_FLOAT64;
        } else if (type.getSimpleName().toLowerCase().equals("byte")) {
            return ProductData.TYPE_INT8;
        } else if (type.getSimpleName().toLowerCase().equals("short")) {
            return ProductData.TYPE_INT16;
        } else if (type.getSimpleName().toLowerCase().equals("integer")) {
            return ProductData.TYPE_INT32;
        } else if (type.getSimpleName().toLowerCase().equals("utc")) {
            return ProductData.TYPE_UTC;
        }
        throw new IllegalArgumentException("Unsupported type '" + type + "'.");
    }

    static boolean isSquareNumber(int number) {
        int temp = (int) Math.sqrt(number);
        return temp * temp == number;
    }

    private boolean isAccessibleBandType(Class<?> type) {
        final String className = type.getSimpleName().toLowerCase();
        return className.equals("float") ||
               className.equals("double") ||
               className.equals("byte") ||
               className.equals("short") ||
               className.equals("integer");
    }

    static class CSVTimeCoding extends PixelTimeCoding {

        private final String dataSourceName;

        public CSVTimeCoding(double[] timeMJD, int rasterWidth, int rasterHeight, String dataSourceName) {
            super(timeMJD, rasterWidth, rasterHeight);
            this.dataSourceName = dataSourceName;
        }

        public String getDataSourceName() {
            return dataSourceName;
        }
    }
}
