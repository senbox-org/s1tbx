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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MeasurementWriter2 {

    private final Map<String, PrintWriter> writerMap;
    private final File outputDir;
    private final String filenamePrefix;
    private final int windowSize;
    private final String expression;
    private PrintWriter productMapWriter;

    public MeasurementWriter2(File outputDir, String filenamePrefix, int windowSize, String expression) throws
                                                                                                        IOException {
        this.outputDir = outputDir;
        this.filenamePrefix = filenamePrefix;
        this.windowSize = windowSize;
        this.expression = expression;
        writerMap = new HashMap<String, PrintWriter>();
    }

    public void write(String productType, Measurement measurement) throws IOException {
        final PrintWriter writer = getMeasurementWriter(productType);
        if (productMapWriter == null) {
            productMapWriter = createProductMapWriter();
        }
    }

    void writeMeasurementFileHeader(PrintWriter writer) {   // package local for testing
        writer.println("# BEAM pixel extraction export table");
        writer.println('#');
        writer.printf("# Window size: %d%n", windowSize);
        if (expression != null) {
            writer.printf("# Expression: %s%n", expression);
        }
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        writer.printf("# Created on:\t%s%n%n", dateFormat.format(new Date()));

        // todo missing table header
    }

    void writeProductMapHeader(PrintWriter printWriter) {   // package local for testing
        printWriter.printf("# Product ID Map%n");
        printWriter.printf("ProductID\tProductType\tProductLocation%n");
    }

    private PrintWriter getMeasurementWriter(String productType) throws IOException {
        PrintWriter writer = writerMap.get(productType);
        if (writer == null) {
            final String fileName = String.format("%s_%s.txt", filenamePrefix, productType);
            File coordinateFile = new File(outputDir, fileName);
            writer = new PrintWriter(new FileOutputStream(coordinateFile), true);
            writerMap.put(productType, writer);
            writeMeasurementFileHeader(writer);
        }
        return writer;
    }

    private PrintWriter createProductMapWriter() throws FileNotFoundException {

        final String fileName = String.format("%s_productIdMap.txt", filenamePrefix);
        final File file = new File(outputDir, fileName);
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(file), true);
        writeProductMapHeader(printWriter);

        return printWriter;
    }

}
