/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PixelTimeCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.csv.dataio.Constants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.image.Raster;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductReaderTest {

    private ProductReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new CsvProductReader(new CsvProductReaderPlugIn());
    }

    @Test
    public void testRead_QuadraticProduct() throws Exception {
        final Product product = readTestProduct("simple_format_4_features.txt");

        assertNotNull(product);
        assertEquals(2, product.getSceneRasterWidth());
        assertEquals(2, product.getSceneRasterHeight());
        testBands(product);
    }

    @Test
    public void testRead_QuadraticProduct_2() throws Exception {
        final Product product = readTestProduct("simple_format_8_features.txt");

        assertNotNull(product);
        assertEquals(3, product.getSceneRasterWidth());
        assertEquals(3, product.getSceneRasterHeight());
        testBands(product);
    }

    @Test
    public void testRead_ProductWithGivenWidth() throws Exception {
        final Product product = readTestProduct("simple_format_sceneRasterWidth.txt");

        assertNotNull(product);
        assertEquals(4, product.getSceneRasterWidth());
        assertEquals(2, product.getSceneRasterHeight());
        testBands(product);
    }

    private void testBands(Product product) {
        final Band[] bands = product.getBands();
        assertEquals(4, bands.length);
        assertEquals("lat", bands[0].getName());
        assertEquals("lon", bands[1].getName());
        assertEquals("radiance_1", bands[2].getName());
        assertEquals("radiance_2", bands[3].getName());

        assertEquals(ProductData.TYPE_FLOAT32, bands[0].getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, bands[1].getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, bands[2].getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, bands[3].getDataType());
    }

    @Test
    public void testRead_ProductWithIntegerValues() throws Exception {
        final Product product = readTestProduct("simple_format_4_integer_features.txt");

        assertNotNull(product);
        assertEquals(2, product.getSceneRasterWidth());
        assertEquals(2, product.getSceneRasterHeight());

        final Band[] bands = product.getBands();
        assertEquals(3, bands.length);
        assertEquals("class", bands[0].getName());
        assertEquals("radiance", bands[1].getName());
        assertEquals("anotherOne", bands[2].getName());

        assertEquals(ProductData.TYPE_INT32, bands[0].getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, bands[1].getDataType());
        assertEquals(ProductData.TYPE_INT32, bands[2].getDataType());
    }

    @Test
    public void testReadBandRasterData() throws Exception {
        final Product product = readTestProduct("simple_format_example.txt");

        assertEquals(3, product.getSceneRasterWidth());
        assertEquals(2, product.getSceneRasterHeight());

        final Band radiance1 = product.getBand("radiance_1");
        final Band radiance2 = product.getBand("radiance_2");

        final Raster radiance1Data = radiance1.getSourceImage().getData();
        final Raster radiance2Data = radiance2.getSourceImage().getData();

        assertEquals(6, radiance1Data.getDataBuffer().getSize());
        assertEquals(6, radiance2Data.getDataBuffer().getSize());

        assertEquals(Float.NaN, radiance1Data.getSampleFloat(0, 0, 0), 1.0E-6);
        assertEquals(13.4f, radiance2Data.getSampleFloat(0, 0, 0), 1.0E-6);

        assertEquals(18.3f, radiance1Data.getSampleFloat(1, 0, 0), 1.0E-6);
        assertEquals(2.4f, radiance2Data.getSampleFloat(1, 0, 0), 1.0E-6);

        assertEquals(10.5f, radiance1Data.getSampleFloat(2, 0, 0), 1.0E-6);
        assertEquals(10.6f, radiance2Data.getSampleFloat(2, 0, 0), 1.0E-6);

        assertEquals(11.5f, radiance1Data.getSampleFloat(0, 1, 0), 1.0E-6);
        assertEquals(11.6f, radiance2Data.getSampleFloat(0, 1, 0), 1.0E-6);

        assertEquals(Float.NaN, radiance1Data.getSampleFloat(1, 1, 0), 1.0E-6);
        assertEquals(Float.NaN, radiance2Data.getSampleFloat(1, 1, 0), 1.0E-6);

        assertEquals(Float.NaN, radiance1Data.getSampleFloat(2, 1, 0), 1.0E-6);
        assertEquals(Float.NaN, radiance2Data.getSampleFloat(2, 1, 0), 1.0E-6);
    }

    private Product readTestProduct(String name) throws IOException {
        return reader.readProductNodes(getClass().getResource(name).getFile(), null);
    }

    @Test
    public void testInvalidInput() throws Exception {
        try {
            ((CsvProductReader) reader).getProductData(null, new ProductData.UTC());
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().matches("Unsupported type.*"));
        }
    }

    @Test
    public void testGetDataType() throws Exception {
        assertEquals(ProductData.TYPE_ASCII, ((CsvProductReader) reader).getProductDataType(String.class));
        assertEquals(ProductData.TYPE_FLOAT32, ((CsvProductReader) reader).getProductDataType(Float.class));
        assertEquals(ProductData.TYPE_FLOAT64, ((CsvProductReader) reader).getProductDataType(Double.class));
        assertEquals(ProductData.TYPE_INT8, ((CsvProductReader) reader).getProductDataType(Byte.class));
    }

    @Test
    public void testIsSquareNumber() throws Exception {
        assertTrue(CsvProductReader.isSquareNumber(1));
        assertTrue(CsvProductReader.isSquareNumber(4));
        assertTrue(CsvProductReader.isSquareNumber(9));
        assertTrue(CsvProductReader.isSquareNumber(16));
        assertFalse(CsvProductReader.isSquareNumber(2));
        assertFalse(CsvProductReader.isSquareNumber(3));
        assertFalse(CsvProductReader.isSquareNumber(5));
        assertFalse(CsvProductReader.isSquareNumber(10));
        assertFalse(CsvProductReader.isSquareNumber(11));
    }

    // Metadata support not yet implemented, therefore test is ignored
    @Test
    @Ignore
    public void testReadMetaData() throws Exception {
        final Product product = readTestProduct("simple_format_example.txt");
        final MetadataElement[] metadataElements = product.getMetadataRoot().getElements();
        final MetadataElement propertyElement = metadataElements[0];
        final MetadataElement recordElement = metadataElements[1];
        final MetadataAttribute[] properties = propertyElement.getAttributes();
        final MetadataElement[] records = recordElement.getElements();

        assertEquals(2, metadataElements.length);

        assertEquals("Properties", propertyElement.getName());
        assertEquals("Records", recordElement.getName());

        assertEquals(4, properties.length);
        assertEquals(3, records.length);

        assertEquals("geometry1", properties[0].getName());
        assertEquals("POLYGON(1.0, 1.0, 1.1)", properties[0].getData().getElemString());
        assertEquals("geometry2", properties[1].getName());
        assertEquals("POLYGON(2.0, 1.0, 1.1)", properties[1].getData().getElemString());
        assertEquals("separator", properties[2].getName());
        assertEquals(",", properties[2].getData().getElemString());
        assertEquals("crs", properties[3].getName());
        assertTrue(properties[3].getData().getElemString().startsWith("GEOGCS[\"WGS 84\""));

        final MetadataElement firstRecord = records[0];
        assertEquals("0", firstRecord.getName());
        assertEquals(3, firstRecord.getAttributes().length);
        assertEquals("station", firstRecord.getAttributes()[0].getName());
        assertEquals("AMRU1", firstRecord.getAttributes()[0].getData().getElemString());
        assertEquals(ProductData.UTC.parse("2010-06-01T12:45:00", Constants.TIME_PATTERN).getAsDate().getTime(), firstRecord.getAttributeUTC("date_time").getAsDate().getTime());
        assertEquals(ProductData.UTC.parse("2011-06-01T10:45:00", Constants.TIME_PATTERN).getAsDate().getTime(), firstRecord.getAttributeUTC("testTime").getAsDate().getTime());

        final MetadataElement secondRecord = records[1];
        assertEquals("1", secondRecord.getName());
        assertEquals(2, secondRecord.getAttributes().length);
        assertEquals("station", secondRecord.getAttributes()[0].getName());
        assertEquals("AMRU1", secondRecord.getAttributes()[0].getData().getElemString());
        assertEquals(ProductData.UTC.parse("2010-06-01T12:48:00", Constants.TIME_PATTERN).getAsDate().getTime(), secondRecord.getAttributeUTC("date_time").getAsDate().getTime());
        assertNull(secondRecord.getAttribute("testTime"));

        final MetadataElement thirdRecord = records[2];
        assertEquals("2", thirdRecord.getName());
        assertEquals(2, thirdRecord.getAttributes().length);
        assertEquals("station", thirdRecord.getAttributes()[0].getName());
        assertEquals("AMRU2", thirdRecord.getAttributes()[0].getData().getElemString());
        assertNull(thirdRecord.getAttribute("date_time"));
        assertEquals(ProductData.UTC.parse("2011-06-01T12:45:00", Constants.TIME_PATTERN).getAsDate().getTime(), thirdRecord.getAttributeUTC("testTime").getAsDate().getTime());
    }

    @Test
    public void testCreateTimeCoding_firstTimeColumn() throws IOException {
        Product product = readTestProduct("simple_format_no_properties_but_time_column.txt");

        CsvProductReader.CSVTimeCoding timeCoding = (CsvProductReader.CSVTimeCoding) product.getSceneTimeCoding();
        assertNotNull(timeCoding);
        assertEquals("date_time", timeCoding.getDataSourceName());

        assertEquals(2, product.getSceneRasterWidth());
        assertEquals(2, product.getSceneRasterHeight());

        assertEquals("01-JUN-2010 12:45:00.000000", getTimeString(timeCoding, 0.5, 0.5));
        assertEquals("01-JUN-2010 12:48:00.000000", getTimeString(timeCoding, 1.5, 0.5));
        assertEquals("01-JUN-2010 12:47:00.000000", getTimeString(timeCoding, 0.5, 1.5));
        assertEquals("NaN", Double.toString(timeCoding.getMJD(new PixelPos(1.5, 1.5))));
    }

    @Test
    public void testCreateTimeCoding_firstCompleteTimeColumn() throws IOException {
        Product product = readTestProduct("simple_format_no_properties_gaps_in_first_time_column.txt");

        CsvProductReader.CSVTimeCoding timeCoding = (CsvProductReader.CSVTimeCoding) product.getSceneTimeCoding();
        assertNotNull(timeCoding);
        assertEquals("complete_time", timeCoding.getDataSourceName());

        assertEquals(2, product.getSceneRasterWidth());
        assertEquals(2, product.getSceneRasterHeight());

        assertEquals("01-JUN-2011 13:45:00.000000", getTimeString(timeCoding, 0.5, 0.5));
        assertEquals("01-JUN-2011 14:45:00.000000", getTimeString(timeCoding, 1.5, 0.5));
        assertEquals("01-JUN-2011 15:45:00.000000", getTimeString(timeCoding, 0.5, 1.5));
        assertEquals("NaN", Double.toString(timeCoding.getMJD(new PixelPos(1.5, 1.5))));

    }

    @Test
    public void testCreateTimeCoding_timeColumnProperty() throws IOException {
        Product product = readTestProduct("simple_format_with_time_column_property.txt");

        CsvProductReader.CSVTimeCoding timeCoding = (CsvProductReader.CSVTimeCoding) product.getSceneTimeCoding();
        assertNotNull(timeCoding);
        assertEquals("any_name", timeCoding.getDataSourceName());

        assertEquals(2, product.getSceneRasterWidth());
        assertEquals(2, product.getSceneRasterHeight());

        assertEquals("01-JUN-2011 10:45:00.000000", getTimeString(timeCoding, 0.5, 0.5));
        assertEquals("01-JUN-2011 11:45:00.000000", getTimeString(timeCoding, 1.5, 0.5));
        assertEquals("01-JUN-2011 12:45:00.000000", getTimeString(timeCoding, 0.5, 1.5));
        assertEquals("NaN", Double.toString(timeCoding.getMJD(new PixelPos(1.5, 1.5))));

    }

    @Test
    public void testCreateTimeCoding_timeColumnAndTimePatternProperty() throws Exception {
        final Product product = readTestProduct("simple_format_with_time_column_and_time_pattern_property.txt");
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement element = metadataRoot.getElement(Constants.NAME_METADATA_ELEMENT_CSV_HEADER_PROPERTIES);

        assertNotNull(element);
        assertEquals("any_name", element.getAttributeString(Constants.PROPERTY_NAME_TIME_COLUMN));
        assertEquals("yyyy-MM-dd '-TickTock-' HH:mm:ss", element.getAttributeString(Constants.PROPERTY_NAME_TIME_PATTERN));

        CsvProductReader.CSVTimeCoding timeCoding = (CsvProductReader.CSVTimeCoding) product.getSceneTimeCoding();
        assertNotNull(timeCoding);
        assertEquals("any_name", timeCoding.getDataSourceName());

        assertEquals(product.getStartTime().getElemString(), ProductData.UTC.parse("01-JUN-2013 10:45:00").getElemString());
        assertEquals(product.getEndTime().getElemString(), ProductData.UTC.parse("01-JUN-2013 12:45:00").getElemString());
        assertEquals(2, product.getSceneRasterWidth());
        assertEquals(2, product.getSceneRasterHeight());

        assertEquals("01-JUN-2013 10:45:00.000000", getTimeString(timeCoding, 0.5, 0.5));
        assertEquals("01-JUN-2013 11:45:00.000000", getTimeString(timeCoding, 1.5, 0.5));
        assertEquals("01-JUN-2013 12:45:00.000000", getTimeString(timeCoding, 0.5, 1.5));
        assertEquals("NaN", Double.toString(timeCoding.getMJD(new PixelPos(1.5, 1.5))));
    }

    @Test
    public void test2x2csvProduct() throws Exception {
        final String dimap = getClass().getResource("MER_FR__1PNUPA20030808_073810_000000982018_00450_07518_6007.dim").getPath();
        final String csv = getClass().getResource("MER_FR__1PNUPA20030808_073810_000000982018_00450_07518_6007.csv").getPath();
        Product dimPro = ProductIO.readProduct(dimap);
        Product csvPro = ProductIO.readProduct(csv);

        assertEquals("org.esa.snap.core.dataio.dimap.DimapProductReader", dimPro.getProductReader().getClass().getName());
        assertEquals(true, dimPro.containsBand("new_band_2"));
        assertEquals(true, dimPro.containsBand("new_band_3"));

        assertEquals("org.esa.snap.csv.dataio.reader.CsvProductReader", csvPro.getProductReader().getClass().getName());
        assertEquals(true, csvPro.containsBand("new_band_2"));
        assertEquals(true, csvPro.containsBand("new_band_3"));

        Band dimBand2 = dimPro.getBand("new_band_2");
        Band dimBand3 = dimPro.getBand("new_band_3");
        Band csvBand2 = csvPro.getBand("new_band_2");
        Band csvBand3 = csvPro.getBand("new_band_3");


        for (int y = 0; y < dimBand2.getRasterHeight(); y++) {
            for (int x = 0; x < dimBand2.getRasterWidth(); x++) {
                float[] expecteds = dimBand2.readPixels(x, y, 1, 1, new float[1]);
                float[] actuals = csvBand2.readPixels(x, y, 1, 1, new float[1]);
                assertEquals("new_band_2 value at x="+x+", y="+y, expecteds[0], actuals[0], 0.0001f);

                expecteds = dimBand3.readPixels(x, y, 1, 1, new float[1]);
                actuals = csvBand3.readPixels(x, y, 1, 1, new float[1]);
                assertEquals("new_band_3 value at x=" + x + ", y=" + y, expecteds[0], actuals[0], 0.0001f);
            }
        }
    }


    private String getTimeString(PixelTimeCoding timeCoding, double x, double y) {
        return new ProductData.UTC(timeCoding.getMJD(new PixelPos(x, y))).format();
    }
}
