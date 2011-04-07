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

package org.esa.beam.pixex;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ProductUtils;

import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes measurements into CSV text files along with a file containing the paths to the products which
 * contributed to the list of measurements.
 */
public class MeasurementWriter {

    private static final DateFormat DATE_FORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss");
    private static final String MEASUREMENTS_FILE_NAME_PATTERN = "%s_%s_measurements.txt";
    private static final String PRODUCT_MAP_FILE_NAME_PATTERN = "%s_productIdMap.txt";

    private final Map<String, PrintWriter> writerMap;
    private final File outputDir;
    private final String filenamePrefix;
    private final int windowSize;
    private final String expression;
    private final boolean exportExpressionResult;
    private final List<ProductIdentifier> productList;
    private final Map<String, String[]> rasterNamesMap;
    private PrintWriter productMapWriter;
    private boolean exportBands;
    private boolean exportTiePoints;
    private boolean exportMasks;


    public MeasurementWriter(File outputDir, String filenamePrefix, int windowSize, String expression,
                             boolean exportExpressionResult) {
        if (windowSize % 2 == 0) {
            throw new IllegalArgumentException("Window size must be an odd value.");
        }
        this.outputDir = outputDir;
        this.filenamePrefix = filenamePrefix;
        this.windowSize = windowSize;
        this.expression = expression;
        writerMap = new HashMap<String, PrintWriter>();
        exportBands = true;
        exportTiePoints = true;
        exportMasks = true;
        this.exportExpressionResult = exportExpressionResult;
        productList = new ArrayList<ProductIdentifier>();
        rasterNamesMap = new HashMap<String, String[]>(37);
    }


    void setExportBands(boolean exportBands) {
        this.exportBands = exportBands;
    }

    void setExportTiePoints(boolean exportTiePoints) {
        this.exportTiePoints = exportTiePoints;
    }

    void setExportMasks(boolean exportMasks) {
        this.exportMasks = exportMasks;
    }

    public void writeMeasurementRegion(int coordinateID, String coordinateName, int upperLeftX, int upperLeftY,
                                       Product product, Raster validData) throws IOException {
        final int productId = registerProduct(product);
        final PrintWriter writer = getMeasurementWriter(product);
        try {
            final String[] rasterNames = rasterNamesMap.get(product.getProductType());
            final Number[] values = new Number[rasterNames.length];
            Arrays.fill(values, Double.NaN);
            final int numPixels = windowSize * windowSize;
            for (int n = 0; n < numPixels; n++) {
                int x = upperLeftX + n % windowSize;
                int y = upperLeftY + n / windowSize;

                final Measurement measure = createMeasurement(product, productId, coordinateID,
                                                              coordinateName, rasterNames, values, validData, x, y);
                write(writer, measure);
            }
        } finally {
            writer.flush();
        }
        if (writer.checkError()) {
            throw new IOException("Error occurred while writing measurement.");
        }
    }

    private int registerProduct(Product product) throws IOException {
        final ProductIdentifier identifier = ProductIdentifier.create(product);
        if (!productList.contains(identifier)) {
            if (productMapWriter == null) {
                productMapWriter = createProductMapWriter();
            }
            productList.add(identifier);

            final String[] rasterNamesToBeExported = getRasterNamesToBeExported(product);
            final String productType = product.getProductType();
            rasterNamesMap.put(productType, rasterNamesToBeExported);

            productMapWriter.printf("%d\t%s\t%s%n", productList.indexOf(identifier), productType,
                                    identifier.getLocation());

            if (productMapWriter.checkError()) {
                throw new IOException("Error occurred while writing measurement.");
            }

        }
        return productList.indexOf(identifier);
    }

    public void close() {
        final Collection<PrintWriter> writerCollection = writerMap.values();
        for (PrintWriter printWriter : writerCollection) {
            printWriter.close();
        }

        if (productMapWriter != null) {
            productMapWriter.close();
        }
        productMapWriter = null;
    }

    private void write(PrintWriter writer, Measurement measurement) {
        if (expression == null || exportExpressionResult || measurement.isValid()) {
            final boolean withExpression = expression != null && exportExpressionResult;
            writeLine(writer, measurement, withExpression);
        }

    }

    static void writeLine(PrintWriter writer, Measurement measurement, boolean withExpression) {
        if (withExpression) {
            writer.printf(Locale.ENGLISH, "%s\t", String.valueOf(measurement.isValid()));
        }
        final ProductData.UTC time = measurement.getTime();
        String timeString;
        if (time != null) {
            timeString = DATE_FORMAT.format(time.getAsDate());
        } else {
            timeString = " \t ";
        }
        writer.printf(Locale.ENGLISH,
                      "%d\t%d\t%s\t%.6f\t%.6f\t%.3f\t%.3f\t%s",
                      measurement.getProductId(), measurement.getCoordinateID(),
                      measurement.getCoordinateName(),
                      measurement.getLat(), measurement.getLon(),
                      measurement.getPixelX(), measurement.getPixelY(),
                      timeString);
        final Number[] values = measurement.getValues();
        for (Number value : values) {
            if (Double.isNaN(value.doubleValue())) {
                writer.printf(Locale.ENGLISH, "\t%s", "");
            } else {
                writer.printf(Locale.ENGLISH, "\t%s", value);
            }
        }
        writer.println();
    }

    private PrintWriter getMeasurementWriter(Product product) throws IOException {
        String productType = product.getProductType();
        PrintWriter writer = writerMap.get(productType);
        if (writer == null) {
            final String fileName = String.format(MEASUREMENTS_FILE_NAME_PATTERN, filenamePrefix, productType);
            File coordinateFile = new File(outputDir, fileName);
            writer = new PrintWriter(new FileOutputStream(coordinateFile));
            writerMap.put(productType, writer);

            writeMeasurementFileHeader(writer, rasterNamesMap.get(productType));
        }
        return writer;
    }


    private String[] getRasterNamesToBeExported(Product product) {
        final List<String> allRasterList = new ArrayList<String>();
        if (exportBands) {
            Collections.addAll(allRasterList, product.getBandNames());
        }
        if (exportTiePoints) {
            Collections.addAll(allRasterList, product.getTiePointGridNames());
        }
        if (exportMasks) {
            Collections.addAll(allRasterList, product.getMaskGroup().getNodeNames());
        }
        return allRasterList.toArray(new String[allRasterList.size()]);
    }

    private PrintWriter createProductMapWriter() throws FileNotFoundException {

        final String fileName = String.format(PRODUCT_MAP_FILE_NAME_PATTERN, filenamePrefix);
        final File file = new File(outputDir, fileName);
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(file), true);
        writeProductMapHeader(printWriter);

        return printWriter;
    }

    void writeMeasurementFileHeader(PrintWriter writer, String[] variableNames) {   // package local for testing
        writer.printf("# BEAM pixel extraction export table%n");
        writer.printf("#%n");
        writer.printf(Locale.ENGLISH, "# Window size: %d%n", windowSize);
        if (expression != null) {
            writer.printf("# Expression: %s%n", expression);
        }

        final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
        writer.printf(Locale.ENGLISH, "# Created on:\t%s%n%n", dateFormat.format(new Date()));

        if (expression != null && exportExpressionResult) {
            writer.print("Expression result\t");
        }
        writer.print(
                "ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)");
        for (String name : variableNames) {
            writer.printf(Locale.ENGLISH, "\t%s", name);
        }
        writer.println();
    }

    void writeProductMapHeader(PrintWriter printWriter) {   // package local for testing
        printWriter.printf("# Product ID Map%n");
        printWriter.printf("ProductID\tProductType\tProductLocation%n");
    }

    private static Measurement createMeasurement(Product product, int productId,
                                                 int coordinateID,
                                                 String coordinateName, String[] rasterNames, Number[] values,
                                                 Raster validData, int x, int y) throws IOException {
        for (int i = 0; i < rasterNames.length; i++) {
            RasterDataNode raster = product.getRasterDataNode(rasterNames[i]);
            if (raster != null && product.containsPixel(x, y)) {
                if (!raster.isPixelValid(x, y)) {
                    values[i] = Double.NaN;
                } else if (raster.isFloatingPointType()) {
                    double[] temp = new double[1];
                    raster.readPixels(x, y, 1, 1, temp);
                    values[i] = temp[0];
                } else {
                    int[] temp = new int[1];
                    raster.readPixels(x, y, 1, 1, temp);
                    if (raster instanceof Mask) {
                        values[i] = temp[0] == 0 ? 0 : 1; // normalize to 0 for false and 1 for true
                    } else {
                        if (raster.getDataType() == ProductData.TYPE_UINT32) {
                            values[i] = temp[0] & 0xffffL;
                        } else {
                            values[i] = temp[0];
                        }
                    }
                }
            }
        }
        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        GeoPos currentGeoPos = product.getGeoCoding().getGeoPos(pixelPos, null);
        boolean isValid = validData.getSample(x, y, 0) != 0;
        final ProductData.UTC scanLineTime = ProductUtils.getScanLineTime(product, pixelPos.y);

        return new Measurement(coordinateID, coordinateName, productId,
                               pixelPos.x, pixelPos.y, scanLineTime, currentGeoPos, values,
                               isValid);
    }

}
