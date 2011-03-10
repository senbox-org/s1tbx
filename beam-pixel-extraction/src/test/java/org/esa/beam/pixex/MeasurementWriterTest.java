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

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
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
        final int windowSize = 1;
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testFileCreation", windowSize, "", true);

        File productMapFile = new File(outputDir, "testFileCreation_productIdMap.txt");
        File t1CoordFile = new File(outputDir, "testFileCreation_T1_measurements.txt");

        assertEquals(0, outputDir.listFiles().length);
        assertFalse(productMapFile.exists());
        assertFalse(t1CoordFile.exists());

        final Product p1 = createTestProduct("N1", "T1", new String[0], 360, 180);
        writeRegion(writer, p1, 1, windowSize);

        assertEquals(2, outputDir.listFiles().length);
        assertTrue(productMapFile.exists());
        assertTrue(t1CoordFile.exists());

        final Product p2 = createTestProduct("N2", "T2", new String[0], 360, 180);
        writeRegion(writer, p2, 1, windowSize);

        File t2CoordFile = new File(outputDir, "testFileCreation_T2_measurements.txt");
        assertEquals(3, outputDir.listFiles().length);
        assertTrue(productMapFile.exists());
        assertTrue(t1CoordFile.exists());
        assertTrue(t2CoordFile.exists());
    }

    private void writeRegion(MeasurementWriter writer, Product p1, int coordId, int windowSize) throws IOException {
        final int pixelX = 20;
        final int pixelY = 42;
        final byte validValue = (byte) (coordId % 2 == 0 ? -1 : 0);
        final RenderedOp renderedOp = ConstantDescriptor.create((float) p1.getSceneRasterWidth(),
                                                                (float) p1.getSceneRasterHeight(),
                                                                new Byte[]{validValue}, null);
        final Raster validData = renderedOp.getData(new Rectangle(pixelX, pixelY, windowSize, windowSize));

        writer.writeMeasurementRegion(coordId, "coord" + coordId, pixelX, pixelY, p1, validData);
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
    public void testMeasurementWritingWithNaNValues() throws Exception {

        final Number[] values = new Number[]{12.4, Double.NaN, 1.0345, 7};
        final Measurement measurement = new Measurement(12, "Name", 27, 13.5f, 0.5f,
                                                        ProductData.UTC.parse("2005-07-09T10:12:03",
                                                                              "yyyy-MM-dd'T'hh:mm:ss"),
                                                        new GeoPos(30, 60),
                                                        values, true);
        final StringWriter writer = new StringWriter();
        MeasurementWriter.writeLine(new PrintWriter(writer), measurement, false);
        final String line = writer.getBuffer().toString();
        final String expected = String.format(
                "27\t12\tName\t30.000000\t60.000000\t13.500\t0.500\t2005-07-09\t10:12:03\t12.4\t\t1.0345\t7%n");
        assertEquals(expected, line);
    }

    @Test
    public void testWritingMeasurements() throws Exception {
        final int windowSize = 1;
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testWritingMeasurements", windowSize, null,
                                                               true);
        writer.setExportMasks(false);
        final String[] varNames = {"abc", "def"};
        final Product testProduct1 = createTestProduct("N1", "T1", varNames, 360, 180);
        final Product testProduct2 = createTestProduct("N2", "T1", varNames, 360, 180);
        final Product testProduct3 = createTestProduct("N3", "T2", varNames, 360, 180);
        writeRegion(writer, testProduct1, 1, windowSize);
        writeRegion(writer, testProduct1, 2, windowSize);
        writeRegion(writer, testProduct2, 3, windowSize);
        writeRegion(writer, testProduct3, 4, windowSize);

        File t1CoordFile = new File(outputDir, "testWritingMeasurements_T1_measurements.txt");
        BufferedReader reader = new BufferedReader(new FileReader(t1CoordFile));
        skipLines(reader, 6);    //skip file header and table header lines
        assertMeasurementEquals(getMeasurement(1, 0), reader.readLine(), false);
        assertMeasurementEquals(getMeasurement(2, 0), reader.readLine(), false);
        assertMeasurementEquals(getMeasurement(3, 1), reader.readLine(), false);

        File t2CoordFile = new File(outputDir, "testWritingMeasurements_T2_measurements.txt");
        reader = new BufferedReader(new FileReader(t2CoordFile));
        skipLines(reader, 6);    //skip file header and table header lines
        assertMeasurementEquals(getMeasurement(4, 2), reader.readLine(), false);
    }

    @Test
    public void testWritingMeasurementsWithExpression() throws Exception {
        final boolean withExpression = true;
        final int windowSize = 1;
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testWritingMeasurementsWithExpression",
                                                               windowSize, "Is Valid", withExpression);
        writer.setExportMasks(false);
        final String[] varNames = {"abc", "def"};
        final Product testProduct = createTestProduct("N1", "T1", varNames, 360, 180);
        writeRegion(writer, testProduct, 1, windowSize);
        writeRegion(writer, testProduct, 2, windowSize);

        File t1CoordFile = new File(outputDir, "testWritingMeasurementsWithExpression_T1_measurements.txt");
        BufferedReader reader = new BufferedReader(new FileReader(t1CoordFile));
        skipLines(reader, 7);    //skip file header and table header lines
        assertMeasurementEquals(getMeasurement(1, 0), reader.readLine(), withExpression);
        assertMeasurementEquals(getMeasurement(2, 0), reader.readLine(), withExpression);
    }

    @Test
    public void testWritingProductMap() throws Exception {
        final int windowSize = 3;
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testWritingProductMap", windowSize, null,
                                                               true);
        final String[] varNames = {"abc", "def"};
        final Product testProduct = createTestProduct("N1", "T1", varNames, 360, 180);
        testProduct.setFileLocation(new File("somewhere/on/disk.txt"));
        writeRegion(writer, testProduct, 1, windowSize);

        File t1CoordFile = new File(outputDir, "testWritingProductMap_productIdMap.txt");
        final BufferedReader reader = new BufferedReader(new FileReader(t1CoordFile));
        skipLines(reader, 2);    //skip file header and table header lines
        String line = reader.readLine();
        assertNotNull("Nothing written to ProductMap.", line);
        assertFalse(line.isEmpty());
        assertProductMapEntryEquals(0, "T1", testProduct.getFileLocation().getAbsolutePath(), line);

        writeRegion(writer, testProduct, 2, windowSize);
        assertNull("No new entry expected.", reader.readLine());

        final Product testProduct2 = createTestProduct("N2", "T1", varNames, 360, 180);
        testProduct2.setFileLocation(new File("somewhere/on/disk2.txt"));
        writeRegion(writer, testProduct2, 1, windowSize);
        final Product testProduct3 = createTestProduct("N2", "T2", varNames, 360, 180);
        testProduct3.setFileLocation(new File("somewhere/on/disk3.txt"));
        writeRegion(writer, testProduct3, 1, windowSize);

        line = reader.readLine();
        assertNotNull("Nothing written to ProductMap.", line);
        assertFalse(line.isEmpty());
        assertProductMapEntryEquals(1, "T1", testProduct2.getFileLocation().getAbsolutePath(), line);
        line = reader.readLine();
        assertNotNull("Nothing written to ProductMap.", line);
        assertFalse(line.isEmpty());
        assertProductMapEntryEquals(2, "T2", testProduct3.getFileLocation().getAbsolutePath(), line);

    }

    private void assertProductMapEntryEquals(int productId, String productType, String location, String line) {
        final Scanner scanner = new Scanner(line);
        scanner.useLocale(Locale.ENGLISH);
        scanner.useDelimiter("\t");
        assertEquals(productId, scanner.nextInt());
        assertEquals(productType, scanner.next());
        assertEquals(location, scanner.next());
        assertFalse("Too much information on single line.", scanner.hasNext());
    }


    @Test
    public void testClosing() throws Exception {
        final int windowSize = 1;
        final MeasurementWriter writer = new MeasurementWriter(outputDir, "testClosing", windowSize, null, true);
        final Product testProduct = createTestProduct("N1", "T1", new String[0], 360, 180);
        writeRegion(writer, testProduct, 1, windowSize);

        writer.close();

        try {
            writeRegion(writer, testProduct, 2, windowSize);
            fail("IOException expected: The writer is closed.");
        } catch (IOException ignored) {
        }
    }

    private void assertMeasurementEquals(Measurement measurement, String line, boolean withExpression) throws
                                                                                                       ParseException {
        final Scanner scanner = new Scanner(line);
        scanner.useLocale(Locale.ENGLISH);
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
        final String date = scanner.next().trim();
        final String time = scanner.next().trim();
        if (!date.isEmpty() && !time.isEmpty()) {
            final String dateTime = ProductData.UTC.parse(date + " " + time, "yyyy-MM-dd HH:mm:ss").format();
            assertEquals(measurement.getTime().format(), dateTime);
        }
        int numValues = 0;
        while (scanner.hasNextFloat()) {
            numValues++;
            scanner.nextFloat();
        }
        assertEquals(measurement.getValues().length, numValues);

    }

    private void skipLines(BufferedReader reader, int numLines) throws IOException {
        for (int i = 0; i < numLines; i++) {
            reader.readLine();
        }
    }

    public static Product createTestProduct(String name, String type, String[] bandNames, int width, int height) throws
                                                                                                                 FactoryException,
                                                                                                                 TransformException {
        Rectangle bounds = new Rectangle(width, height);
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

    private static Measurement getMeasurement(int coordId, int productId) throws ParseException {
        return new Measurement(coordId, "coord" + coordId, productId,
                               20.5f, 42.5f,
                               ProductData.UTC.parse("12-MAR-2008 17:12:56"),
                               new GeoPos(47.5f, -159.5f),
                               new Float[]{12.34f, 1234.56f}, coordId % 2 == 0);
    }
}
