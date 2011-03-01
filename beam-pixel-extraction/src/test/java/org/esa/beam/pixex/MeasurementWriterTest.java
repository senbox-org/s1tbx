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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

import static junit.framework.Assert.*;

@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
public class MeasurementWriterTest {

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
    public void testFileCreation() throws Exception {
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testFileCreation", 1, "", true);

        File productMapFile = new File(outputDir, "testFileCreation_productIdMap.txt");
        File t1CoordFile = new File(outputDir, "testFileCreation_T1_measurements.txt");

        assertEquals(0, outputDir.listFiles().length);
        assertFalse(productMapFile.exists());
        assertFalse(t1CoordFile.exists());

        final Product p1 = createTestProduct("N1", "T1", new String[0]);
        final Measurement measure1 = getMeasurement(1, writer.registerProduct(p1));
        writer.write(p1, measure1);

        assertEquals(2, outputDir.listFiles().length);
        assertTrue(productMapFile.exists());
        assertTrue(t1CoordFile.exists());

        final Product p2 = createTestProduct("N2", "T2", new String[0]);
        final Measurement measure2 = getMeasurement(1, writer.registerProduct(p2));
        writer.write(p2, measure2);

        File t2CoordFile = new File(outputDir, "testFileCreation_T2_measurements.txt");
        assertEquals(3, outputDir.listFiles().length);
        assertTrue(productMapFile.exists());
        assertTrue(t1CoordFile.exists());
        assertTrue(t2CoordFile.exists());
    }

    @Test
    public void testProductMapFileHeader() throws Exception {
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testProductMapFileHeader", 1, "", true);
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
    public void testMeasurementFileHeaderWithExpression() throws Exception {
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testMeasurementFileHeader", 9,
                                                               "expression", true);
        final StringWriter stringWriter = new StringWriter(200);
        final String[] variableNames = new String[]{"rad_1", "rad_2", "uncert"};
        writer.writeMeasurementFileHeader(new PrintWriter(stringWriter), variableNames);

        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        String line = reader.readLine();
        assertEquals("# BEAM pixel extraction export table", line);
        line = reader.readLine();
        assertEquals("#", line);
        line = reader.readLine();
        assertEquals("# Window size: 9", line);
        line = reader.readLine();
        assertEquals("# Expression: expression", line);
        line = reader.readLine();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        assertTrue(line.startsWith("# Created on:\t" + dateFormat.format(new Date())));
        line = reader.readLine();
        assertTrue(line.isEmpty());
        line = reader.readLine();
        assertEquals("Expression result\tProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\t" +
                     "Date(yyyy-MM-dd)\tTime(HH:mm:ss)\trad_1\trad_2\tuncert",
                     line);

    }

    @Test
    public void testMeasurementFileHeaderWithExpression_NotExporting() throws Exception {
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testMeasurementFileHeader", 9,
                                                               "expression", false);
        final StringWriter stringWriter = new StringWriter(200);
        final String[] variableNames = new String[]{"rad_1", "rad_2", "uncert"};
        writer.writeMeasurementFileHeader(new PrintWriter(stringWriter), variableNames);

        final BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()));
        String line = reader.readLine();
        assertEquals("# BEAM pixel extraction export table", line);
        line = reader.readLine();
        assertEquals("#", line);
        line = reader.readLine();
        assertEquals("# Window size: 9", line);
        line = reader.readLine();
        assertEquals("# Expression: expression", line);
        line = reader.readLine();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        assertTrue(line.startsWith("# Created on:\t" + dateFormat.format(new Date())));
        line = reader.readLine();
        assertTrue(line.isEmpty());
        line = reader.readLine();
        assertEquals("ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\t" +
                     "Date(yyyy-MM-dd)\tTime(HH:mm:ss)\trad_1\trad_2\tuncert",
                     line);

    }

    @Test
    public void testMeasurementFileHeaderWithoutExpression() throws Exception {
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testMeasurementFileHeader", 3, null,
                                                               false);
        final StringWriter stringWriter = new StringWriter(200);
        final String[] variableNames = new String[]{"varA", "varB", "var C"};
        writer.writeMeasurementFileHeader(new PrintWriter(stringWriter), variableNames);

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
        line = reader.readLine();
        assertTrue(line.isEmpty());
        line = reader.readLine();
        assertEquals("ProdID\tCoordID\tName\tLatitude\tLongitude\tPixelX\tPixelY\t" +
                     "Date(yyyy-MM-dd)\tTime(HH:mm:ss)\tvarA\tvarB\tvar C",
                     line);
    }

    @Test
    public void testWritingMeasurements() throws Exception {
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testWritingMeasurements", 3, null, true);
        final String[] varNames = {"abc", "def"};
        final Product testProduct1 = createTestProduct("N1", "T1", varNames);
        final Product testProduct2 = createTestProduct("N2", "T1", varNames);
        final Product testProduct3 = createTestProduct("N3", "T2", varNames);
        final int id1 = writer.registerProduct(testProduct1);
        final int id2 = writer.registerProduct(testProduct2);
        final int id3 = writer.registerProduct(testProduct3);
        final Measurement measurement1 = getMeasurement(1, id1);
        final Measurement measurement2 = getMeasurement(2, id1);
        final Measurement measurement3 = getMeasurement(3, id2);
        final Measurement measurement4 = getMeasurement(4, id3);
        writer.write(testProduct1, measurement1);
        writer.write(testProduct1, measurement2);
        writer.write(testProduct2, measurement3);
        writer.write(testProduct3, measurement4);

        File t1CoordFile = new File(outputDir, "testWritingMeasurements_T1_measurements.txt");
        BufferedReader reader = new BufferedReader(new FileReader(t1CoordFile));
        skipLines(reader, 6);    //skip file header and table header lines
        assertMeasurementEquals(measurement1, reader.readLine(), false);
        assertMeasurementEquals(measurement2, reader.readLine(), false);
        assertMeasurementEquals(measurement3, reader.readLine(), false);

        File t2CoordFile = new File(outputDir, "testWritingMeasurements_T2_measurements.txt");
        reader = new BufferedReader(new FileReader(t2CoordFile));
        skipLines(reader, 6);    //skip file header and table header lines
        assertMeasurementEquals(measurement4, reader.readLine(), false);
    }

    @Test
    public void testWritingMeasurementsWithExpression() throws Exception {
        final boolean withExpression = true;
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testWritingMeasurementsWithExpression",
                                                               3, "Is Valid", withExpression);
        final String[] varNames = {"abc", "def"};
        final Product testProduct = createTestProduct("N1", "T1", varNames);
        final int testProductId = writer.registerProduct(testProduct);
        final Measurement measurement1 = getMeasurement(1, testProductId);
        final Measurement measurement2 = getMeasurement(2, testProductId);
        writer.write(testProduct, measurement1);
        writer.write(testProduct, measurement2);

        File t1CoordFile = new File(outputDir, "testWritingMeasurementsWithExpression_T1_measurements.txt");
        BufferedReader reader = new BufferedReader(new FileReader(t1CoordFile));
        skipLines(reader, 7);    //skip file header and table header lines
        assertMeasurementEquals(measurement1, reader.readLine(), withExpression);
        assertMeasurementEquals(measurement2, reader.readLine(), withExpression);
    }

    @Test
    public void testWritingProductMap() throws Exception {
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testWritingProductMap", 3, null, true);
        final String[] varNames = {"abc", "def"};
        final Product testProduct = createTestProduct("N1", "T1", varNames);
        testProduct.setFileLocation(new File("somewhere/on/disk.txt"));
        int testProductId = writer.registerProduct(testProduct);
        final Measurement measurement1 = getMeasurement(1, testProductId);
        writer.write(testProduct, measurement1);

        File t1CoordFile = new File(outputDir, "testWritingProductMap_productIdMap.txt");
        final BufferedReader reader = new BufferedReader(new FileReader(t1CoordFile));
        skipLines(reader, 2);    //skip file header and table header lines
        String line = reader.readLine();
        assertNotNull("Nothing written to ProductMap.", line);
        assertFalse(line.isEmpty());
        assertProductMapEntryEquals(0, "T1", testProduct.getFileLocation().getAbsolutePath(), line);
        final Measurement measurement2 = getMeasurement(2, testProductId);
        writer.write(testProduct, measurement2);
        assertNull("No new entry expected.", reader.readLine());

        final Product testProduct2 = createTestProduct("N2", "T1", varNames);
        testProduct2.setFileLocation(new File("somewhere/on/disk2.txt"));
        writer.write(testProduct2, getMeasurement(1, writer.registerProduct(testProduct2)));
        line = reader.readLine();
        assertNotNull("Nothing written to ProductMap.", line);
        assertFalse(line.isEmpty());
        assertProductMapEntryEquals(1, "T1", testProduct2.getFileLocation().getAbsolutePath(), line);

    }

    private void assertProductMapEntryEquals(int productId, String productType, String location, String line) {
        final Scanner scanner = new Scanner(line);
        scanner.useDelimiter("\t");
        assertEquals(productId, scanner.nextInt());
        assertEquals(productType, scanner.next());
        assertEquals(location, scanner.next());
    }


    @Test
    public void testClosing() throws Exception {
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testClosing", 3, null, true);
        final Product testProduct = createTestProduct("N1", "T1", new String[0]);
        writer.write(testProduct, getMeasurement(1, -1));

        writer.close();

        try {
            writer.write(testProduct, getMeasurement(2, -1));
            fail("IOException expected: The writer is closed.");
        } catch (IOException ignored) {
        }
    }

    private void assertMeasurementEquals(Measurement measurement, String line, boolean withExpression) throws
                                                                                                       ParseException {
        final Scanner scanner = new Scanner(line);
        scanner.useDelimiter("\t");
        if (withExpression) {
            assertEquals(measurement.isValid(), scanner.nextBoolean());
        }
        assertEquals(measurement.getProductId(), scanner.nextInt());
        assertEquals(measurement.getCoordinateID(), scanner.nextInt());
        assertEquals(measurement.getCoordinateName(), scanner.next());
        assertEquals(measurement.getLat(), scanner.nextFloat());
        assertEquals(measurement.getLon(), scanner.nextFloat());
        assertEquals(measurement.getPixelX(), scanner.nextFloat());
        assertEquals(measurement.getPixelY(), scanner.nextFloat());
        final String date = scanner.next();
        final String time = scanner.next();
        final String dateTime = ProductData.UTC.parse(date + " " + time, "yyyy-MM-dd HH:mm:ss").format();
        assertEquals(measurement.getTime().format(), dateTime);
        final Number[] values = measurement.getValues();
        for (Number value : values) {
            assertEquals(value.floatValue(), scanner.nextFloat());
        }

    }

    private void skipLines(BufferedReader reader, int numLines) throws IOException {
        for (int i = 0; i < numLines; i++) {
            reader.readLine();
        }
    }

    public static Measurement getMeasurement(int coordId, int productId) throws ParseException {
        return new Measurement(coordId, "coord" + coordId, productId,
                               20.5f, 42.8f,
                               ProductData.UTC.parse("12-MAR-2008 17:12:56"),
                               new GeoPos(56.78f, -10.23f),
                               new Float[]{12.34f, 1234.56f}, coordId % 2 == 0);
    }

    public static Product createTestProduct(String name, String type, String[] bandNames) throws FactoryException,
                                                                                                 TransformException {
        Rectangle bounds = new Rectangle(360, 180);
        Product product = new Product(name, type, bounds.width, bounds.height);
        AffineTransform i2mTransform = new AffineTransform();
        final int northing = 90;
        final int easting = -180;
        i2mTransform.translate(easting, northing);
        final double scaleX = 360 / bounds.width;
        final double scaleY = 180 / bounds.height;
        i2mTransform.scale(scaleX, -scaleY);
        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, bounds, i2mTransform);
        product.setGeoCoding(geoCoding);
        for (int i = 0; i < bandNames.length; i++) {
            Band band = product.addBand(bandNames[i], ProductData.TYPE_FLOAT32);
            band.setSourceImage(ConstantDescriptor.create((float) bounds.width, (float) bounds.height,
                                                          new Float[]{(float) i}, null));
        }
        return product;
    }
}
