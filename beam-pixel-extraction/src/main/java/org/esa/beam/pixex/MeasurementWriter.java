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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private PrintWriter productMapWriter;
    private boolean exportBands;
    private boolean exportTiePoints;
    private boolean exportMasks;
    private List<ProductDescription> productList;

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
        productList = new ArrayList<ProductDescription>();
    }

    public void setExportBands(boolean exportBands) {
        this.exportBands = exportBands;
    }

    public void setExportTiePoints(boolean exportTiePoints) {
        this.exportTiePoints = exportTiePoints;
    }

    public void setExportMasks(boolean exportMasks) {
        this.exportMasks = exportMasks;
    }

    public void write(Product product, Measurement measurement) throws IOException {
        final PrintWriter writer = getMeasurementWriter(product);
        write(writer, measurement);

        if (writer.checkError()) {
            throw new IOException("Error occurred while writing measurement.");
        }
    }

    public int registerProduct(Product product) throws IOException {
        final ProductDescription description = ProductDescription.create(product);
        if (!productList.contains(description)) {
            if (productMapWriter == null) {
                productMapWriter = createProductMapWriter();
            }
            productList.add(description);
            productMapWriter.printf("%d\t%s\t%s", productList.indexOf(description), product.getProductType(),
                                    description.getLocation());

            if (productMapWriter.checkError()) {
                throw new IOException("Error occurred while writing measurement.");
            }

        }
        return productList.indexOf(description);
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
            if (expression != null && exportExpressionResult) {
                writer.printf("%s\t", String.valueOf(measurement.isValid()));
            }
            final ProductData.UTC time = measurement.getTime();
            String timeString;
            if (time != null) {
                timeString = DATE_FORMAT.format(time.getAsDate());
            } else {
                timeString = " \t ";
            }
            writer.printf("%d\t%d\t%s\t%s\t%s\t%s\t%s\t%s",
                          measurement.getProductId(), measurement.getCoordinateID(),
                          measurement.getCoordinateName(),
                          measurement.getLat(), measurement.getLon(),
                          measurement.getPixelX(), measurement.getPixelY(),
                          timeString);
            final Number[] values = measurement.getValues();
            for (Number value : values) {
                writer.printf("\t%s", value);
            }
            writer.println();
        }

    }

    private PrintWriter getMeasurementWriter(Product product) throws IOException {
        String productType = product.getProductType();
        PrintWriter writer = writerMap.get(productType);
        if (writer == null) {
            final String fileName = String.format(MEASUREMENTS_FILE_NAME_PATTERN, filenamePrefix, productType);
            File coordinateFile = new File(outputDir, fileName);
            writer = new PrintWriter(new FileOutputStream(coordinateFile), true);
            writerMap.put(productType, writer);

            writeMeasurementFileHeader(writer, getRasterNamesToBeExported(product));
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
        writer.println("# BEAM pixel extraction export table");
        writer.println('#');
        writer.printf("# Window size: %d%n", windowSize);
        if (expression != null) {
            writer.printf("# Expression: %s%n", expression);
        }
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        writer.printf("# Created on:\t%s%n%n", dateFormat.format(new Date()));

        if (expression != null && exportExpressionResult) {
            writer.print("Expression result\t");
        }
        writer.print(
                "ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\tDate(yyyy-MM-dd)\tTime(HH:mm:ss)");
        for (String name : variableNames) {
            writer.append(String.format("\t%s", name));
        }
        writer.println();
    }

    void writeProductMapHeader(PrintWriter printWriter) {   // package local for testing
        printWriter.printf("# Product ID Map%n");
        printWriter.printf("ProductID\tProductType\tProductLocation%n");
    }

}
