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
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static junit.framework.Assert.*;

public class MeasurementWriter2Test {

    private File outputDir;

    @Before
    public void setup() throws IOException {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        outputDir = new File(tmpDir, getClass().getSimpleName());
        if (!outputDir.mkdir()) { // already exists, so delete contents
            for (File file : outputDir.listFiles()) {
                file.delete();
            }
        }
    }

    @Test
    public void testFileCreation() throws ParseException, IOException {
        final MeasurementWriter2 writer = new MeasurementWriter2(outputDir, "testFileCreation", 1, "");
        final Measurement measurement = new Measurement(1, "coord1", 21, 20.5f, 42.8f,
                                                        ProductData.UTC.parse("12-MAR-2008 17:12:56"),
                                                        new GeoPos(56.78f, -10.23f),
                                                        new Float[]{12.34f, 1234.56f}, true);

        File productMapFile = new File(outputDir, "testFileCreation_productIdMap.txt");
        File t1CoordFile = new File(outputDir, "testFileCreation_T1.txt");

        assertEquals(0, outputDir.listFiles().length);
        assertFalse(productMapFile.exists());
        assertFalse(t1CoordFile.exists());

        writer.write("T1", measurement);

        assertEquals(2, outputDir.listFiles().length);
        assertTrue(productMapFile.exists());
        assertTrue(t1CoordFile.exists());

        writer.write("T2", measurement);

        File t2CoordFile = new File(outputDir, "testFileCreation_T2.txt");
        assertEquals(3, outputDir.listFiles().length);
        assertTrue(productMapFile.exists());
        assertTrue(t1CoordFile.exists());
        assertTrue(t2CoordFile.exists());
    }

    @Test
    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    public void testProductMapFileHeader() throws ParseException, IOException {
        final MeasurementWriter2 writer = new MeasurementWriter2(outputDir, "testProductMapFileHeader", 1, "");
        final StringWriter stringWriter = new StringWriter(200);
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        writer.writeProductMapHeader(printWriter);

        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        String line = reader.readLine();
        assertEquals("# Product ID Map", line);
        line = reader.readLine();
        assertEquals("ProductID\tProductType\tProductLocation", line);
    }

    @Test
    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    public void testMeasurementFileHeaderWithExpression() throws ParseException, IOException {
        final MeasurementWriter2 writer = new MeasurementWriter2(outputDir, "testMeasurementFileHeader", 3,
                                                                 "expression");
        final StringWriter stringWriter = new StringWriter(200);
        writer.writeMeasurementFileHeader(new PrintWriter(stringWriter));

        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        String line = reader.readLine();
        assertEquals("# BEAM pixel extraction export table", line);
        line = reader.readLine();
        assertEquals("#", line);
        line = reader.readLine();
        assertEquals("# Window size: 3", line);
        line = reader.readLine();
        assertEquals("# Expression: expression", line);
        line = reader.readLine();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        assertTrue(line.startsWith("# Created on:\t" + dateFormat.format(new Date())));
    }

    @Test
    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    public void testMeasurementFileHeaderWithoutExpression() throws ParseException, IOException {
        final MeasurementWriter2 writer = new MeasurementWriter2(outputDir, "testMeasurementFileHeader", 3, null);
        final StringWriter stringWriter = new StringWriter(200);
        writer.writeMeasurementFileHeader(new PrintWriter(stringWriter));

        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        String line = reader.readLine();
        assertEquals("# BEAM pixel extraction export table", line);
        line = reader.readLine();
        assertEquals("#", line);
        line = reader.readLine();
        assertEquals("# Window size: 3", line);
        line = reader.readLine();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        assertTrue(line.startsWith("# Created on:\t" + dateFormat.format(new Date())));
    }
}
