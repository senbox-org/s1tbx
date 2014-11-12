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

package org.esa.beam.binning.operator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.CellProcessorConfig;
import org.esa.beam.binning.DataPeriod;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.aggregators.AggregatorMinMax;
import org.esa.beam.binning.aggregators.AggregatorPercentile;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.main.GPT;
import org.esa.beam.util.converters.JtsGeometryConverter;
import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;

import static java.lang.Math.sqrt;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Test that creates a local and a global L3 product from 5 source files.
 * The {@link BinningOp} is tested directly using an operator instance,
 * and indirectly using the GPF facade and the GPT command-line tool.
 *
 * @author Norman Fomferra
 */
public class BinningOpTest {

    static final File TESTDATA_DIR = new File("target/binning-test-io");

    static {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    @Before
    public void setUp() throws Exception {
        TESTDATA_DIR.mkdirs();

        // @todo 1 tb/tb should read like the lines commented below .... see todo in tearDown()
//        if (!TESTDATA_DIR.mkdirs()) {
//            fail("Can't create test I/O directory: " + TESTDATA_DIR);
//        }
        if (!TESTDATA_DIR.isDirectory()) {
            fail("Can't create test I/O directory: " + TESTDATA_DIR);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (!FileUtils.deleteTree(TESTDATA_DIR)) {
            // @todo 1 tb/tb check why this fails. I suppose it has to do with the copy operation in the binning op
            // BinningOp line 595 - need to enable the fail after this has been clarified.
            //fail("Warning: failed to completely delete test I/O directory:" + TESTDATA_DIR);
            System.err.println("Warning: failed to completely delete test I/O directory:" + TESTDATA_DIR);
        }
    }

    @Test
    public void testMetadataGeneration() throws Exception {
        BinningOp binningOp = createBinningOp();

        binningOp.setSourceProducts(createSourceProduct(1, 0.1F),
                createSourceProduct(2, 0.2F),
                createSourceProduct(3, 0.3F));

        binningOp.setStartDateTime("2002-01-01");
        binningOp.setPeriodDuration(10.0);
        binningOp.setAggregatorConfigs(chlAgg(), p70Agg());
        binningOp.setNumRows(180);
        binningOp.setMaskExpr("true");
        binningOp.setOutputFile(getTestFile("target-1.dim").getPath());
        binningOp.setOutputType("Product");
        binningOp.setOutputFormat("BEAM-DIMAP");
        binningOp.setMetadataAggregatorName("NAME");

        binningOp.setParameter("metadataTemplateDir", TESTDATA_DIR);

        assertNull(binningOp.getMetadataProperties());

        Product targetProduct = binningOp.getTargetProduct();

        try {
            SortedMap<String, String> metadataProperties = binningOp.getMetadataProperties();
            assertNotNull(metadataProperties);

            assertEquals(17, metadataProperties.size());
            Set<String> strings = metadataProperties.keySet();
            String[] names = strings.toArray(new String[strings.size()]);
            String[] expectedNames = {
                    "aggregation_period_duration",
                    "aggregation_period_start",
                    //"aggregator_config.0:outputCounts",
                    //"aggregator_config.0:outputSums",
                    //"aggregator_config.0:targetName",
                    "aggregator_config.0:type",
                    "aggregator_config.0:varName",
                    //"aggregator_config.0:weightCoeff",
                    "aggregator_config.1:percentage",
                    //"aggregator_config.1:targetName",
                    "aggregator_config.1:type",
                    "aggregator_config.1:varName",
                    "mask_expression",
                    "metadata_aggregator_name",
                    "num_rows",
                    "pixel_size_in_km",
                    //"processing_time",
                    "product_name",
                    "region",
                    "software_name",
                    "software_qualified_name",
                    "software_version",
                    "super_sampling"
            };
            assertArrayEquals(expectedNames, names);

            //assertTrue("processing_time", metadataProperties.get("processing_time").startsWith("201"));
            assertEquals("target-1", metadataProperties.get("product_name"));
            assertEquals("Binning", metadataProperties.get("software_name"));
            assertEquals("org.esa.beam.binning.operator.BinningOp", metadataProperties.get("software_qualified_name"));
            assertEquals("1.0", metadataProperties.get("software_version"));
            assertEquals("2002-01-01", metadataProperties.get("aggregation_period_start"));
            assertEquals("LINEARRING (0 0, 0 1, 1 1, 1 0, 0 0)", metadataProperties.get("region"));
            // @todo 2 tb/tb add checks for other properties 2014-10-10

            final MetadataElement metadataRoot = targetProduct.getMetadataRoot();
            final MetadataElement processingGraph = metadataRoot.getElement("Processing_Graph");
            final MetadataElement node_0 = processingGraph.getElement("node.0");
            final MetadataElement sourcesElement = node_0.getElement("sources");
            assertNotNull(sourcesElement);
            assertEquals(3, sourcesElement.getNumElements());
        } finally {
            targetProduct.dispose();
        }
    }

    @Test
    public void testInvalidDates() throws Exception {
        final BinningOp binningOp = createBinningOp();
        binningOp.setStartDateTime("2010-01-01");
        binningOp.setPeriodDuration(-1.0);
        try {
            binningOp.initialize();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().equals("The parameter 'periodDuration' must be a positive value"));
        }
    }

    @Test
    public void testBinningWithEmptyMaskExpression() throws Exception {

        float obs1 = 0.2F;

        final BinningOp binningOp = createBinningOp();
        binningOp.setAggregatorConfigs(chlAgg(), p70Agg());
        binningOp.setNumRows(180);

        JtsGeometryConverter geometryConverter = new JtsGeometryConverter();
        binningOp.setSourceProducts(createSourceProduct(1, obs1));
        binningOp.setStartDateTime("2002-01-01");
        binningOp.setPeriodDuration(10.0);
        binningOp.setMaskExpr("");
        final File targetFile = getTestFile("target-1.dim");
        binningOp.setOutputFile(targetFile.getPath());
        binningOp.setOutputType("Product");
        binningOp.setOutputFormat("BEAM-DIMAP");
        binningOp.setRegion(geometryConverter.parse("POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))"));

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
        targetProduct.dispose();
    }

    @Test
    public void testBinningWhenMaskExpressionIsNull() throws Exception {

        final File targetFile = getTestFile("target-1.dim");
        float obs1 = 0.2F;

        final BinningOp binningOp = createBinningOp();
        binningOp.setAggregatorConfigs(chlAgg(), p70Agg());
        binningOp.setNumRows(180);

        JtsGeometryConverter geometryConverter = new JtsGeometryConverter();
        binningOp.setSourceProducts(createSourceProduct(1, obs1));
        binningOp.setStartDateTime("2002-01-01");
        binningOp.setPeriodDuration(10.0);
        binningOp.setMaskExpr(null);
        binningOp.setOutputFile(targetFile.getPath());
        binningOp.setOutputType("Product");
        binningOp.setOutputFormat("BEAM-DIMAP");
        binningOp.setRegion(geometryConverter.parse("POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))"));

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
        targetProduct.dispose();
    }

    /**
     * The following configuration generates a 1-degree resolution global product (360 x 180 pixels) from 5 observations.
     * Values are only generated for pixels at x=180..181 and y=87..89.
     *
     * @throws Exception if something goes badly wrong
     * @see #testLocalBinning()
     */
    @Test
    public void testGlobalBinning() throws Exception {
        final File targetFile = getTestFile("target-1.dim");

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final BinningOp binningOp = createBinningOp();
        binningOp.setAggregatorConfigs(chlAgg(), p70Agg());
        binningOp.setNumRows(180);
        binningOp.setMaskExpr("true");
        binningOp.setSourceProducts(createSourceProduct(1, obs1),
                createSourceProduct(2, obs2),
                createSourceProduct(3, obs3),
                createSourceProduct(4, obs4),
                createSourceProduct(5, obs5));

        JtsGeometryConverter geometryConverter = new JtsGeometryConverter();
        binningOp.setOutputFile(targetFile.getPath());
        binningOp.setOutputType("Product");
        binningOp.setOutputFormat("BEAM-DIMAP");
        binningOp.setStartDateTime("2002-01-01");
        binningOp.setPeriodDuration(10.0);
        binningOp.setRegion(geometryConverter.parse("POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))"));

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
        try {
            assertGlobalBinningProductIsOk(targetProduct, null, obs1, obs2, obs3, obs4, obs5);
        } finally {
            targetProduct.dispose();
        }
    }

    /**
     * The following configuration generates a 1-degree resolution local product (4 x 4 pixels) from 5 observations.
     * The local region is lon=-1..+3 and lat=-1..+3 degrees.
     * Values are only generated for pixels at x=1..2 and y=1..2.
     *
     * @throws Exception if something goes badly wrong
     * @see #testGlobalBinning()
     */
    @Test
    public void testLocalBinning() throws Exception {

        final File targetFile = getTestFile("target-1.dim");

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final BinningOp binningOp = createBinningOp();
        binningOp.setAggregatorConfigs(chlAgg(), p70Agg());
        binningOp.setNumRows(180);
        binningOp.setMaskExpr("true");
        binningOp.setOutputFile(targetFile.getPath());
        binningOp.setOutputType("Product");
        binningOp.setOutputFormat("BEAM-DIMAP");
        binningOp.setSourceProducts(createSourceProduct(1, obs1),
                createSourceProduct(2, obs2),
                createSourceProduct(3, obs3),
                createSourceProduct(4, obs4),
                createSourceProduct(5, obs5));

        GeometryFactory gf = new GeometryFactory();
        binningOp.setRegion(gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                new Coordinate(-1.0, -1.0),
                new Coordinate(3.0, -1.0),
                new Coordinate(3.0, 3.0),
                new Coordinate(-1.0, 3.0),
                new Coordinate(-1.0, -1.0),
        }), null));
        binningOp.setStartDateTime("2002-01-01");
        binningOp.setPeriodDuration(10.0);

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
        try {
            assertLocalBinningProductIsOk(targetProduct, null, obs1, obs2, obs3, obs4, obs5);
        } finally {
            targetProduct.dispose();
        }
    }


    /**
     * Same as {@link #testGlobalBinning}, but this time via the GPF facade.
     *
     * @throws Exception if something goes badly wrong
     * @see #testLocalBinning()
     */
    @Test
    public void testGlobalBinningViaGPF() throws Exception {

        final File targetFile = getTestFile("target-1.dim");

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("startDateTime", "2002-01-01");
        parameters.put("periodDuration", "10");
        parameters.put("numRows", 180);
        parameters.put("maskExpr", "true");
        parameters.put("aggregatorConfigs", new AggregatorConfig[]{chlAgg(), p70Agg()});
        parameters.put("outputFile", targetFile.getPath());
        parameters.put("outputType", "Product");
        parameters.put("outputFormat", "BEAM-DIMAP");
        parameters.put("region", "POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))");

        final Product targetProduct = GPF.createProduct("Binning", parameters,
                createSourceProduct(1, obs1),
                createSourceProduct(2, obs2),
                createSourceProduct(3, obs3),
                createSourceProduct(4, obs4),
                createSourceProduct(5, obs5));

        assertNotNull(targetProduct);
        try {
            assertGlobalBinningProductIsOk(targetProduct, null, obs1, obs2, obs3, obs4, obs5);
        } finally {
            targetProduct.dispose();
        }
    }

    /**
     * Same as {@link #testLocalBinning}, but this time via the GPF facade.
     *
     * @throws Exception if something goes badly wrong
     * @see #testLocalBinning()
     */
    @Test
    public void testLocalBinningViaGPF() throws Exception {

        final File targetFile = getTestFile("target-1.dim");

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("region", "POLYGON((-1 -1, 3 -1, 3 3, -1 3, -1 -1))");
        parameters.put("startDateTime", "2002-01-01");
        parameters.put("periodDuration", "10");
        parameters.put("numRows", 180);
        parameters.put("maskExpr", "true");
        parameters.put("aggregatorConfigs", new AggregatorConfig[]{chlAgg(), p70Agg()});
        parameters.put("outputFile", targetFile.getPath());
        parameters.put("outputType", "Product");
        parameters.put("outputFormat", "BEAM-DIMAP");

        final Product targetProduct = GPF.createProduct("Binning",
                parameters,
                createSourceProduct(1, obs1),
                createSourceProduct(2, obs2),
                createSourceProduct(3, obs3),
                createSourceProduct(4, obs4),
                createSourceProduct(5, obs5));
        assertNotNull(targetProduct);
        try {
            assertLocalBinningProductIsOk(targetProduct, null, obs1, obs2, obs3, obs4, obs5);
        } finally {
            targetProduct.dispose();
        }
    }

    /**
     * Same as {@link #testGlobalBinning}, but this time via the 'gpt' command-line tool.
     *
     * @throws Exception if something goes badly wrong
     * @see #testLocalBinning()
     */
    @Test
    public void testGlobalBinningViaGPT() throws Exception {

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final File parameterFile = new File(getClass().getResource("BinningParamsGlobal.xml").toURI());
        final File targetFile = getTestFile("output.dim");
        final File sourceFile1 = getTestFile("obs1.dim");
        final File sourceFile2 = getTestFile("obs2.dim");
        final File sourceFile3 = getTestFile("obs3.dim");
        final File sourceFile4 = getTestFile("obs4.dim");
        final File sourceFile5 = getTestFile("obs5.dim");

        ProductIO.writeProduct(createSourceProduct(1, obs1), sourceFile1, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(2, obs2), sourceFile2, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(3, obs3), sourceFile3, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(4, obs4), sourceFile4, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(5, obs5), sourceFile5, "BEAM-DIMAP", false);

        GPT.run(new String[]{
                "Binning",
                "-p", parameterFile.getPath(),
                "-t", targetFile.getPath(),
                sourceFile1.getPath(),
                sourceFile2.getPath(),
                sourceFile3.getPath(),
                sourceFile4.getPath(),
                sourceFile5.getPath(),
        });

        assertTrue(targetFile.exists());

        final Product targetProduct = ProductIO.readProduct(targetFile);
        assertNotNull(targetProduct);
        try {
            assertGlobalBinningProductIsOk(targetProduct, targetFile, obs1, obs2, obs3, obs4, obs5);
        } finally {
            targetProduct.dispose();
        }
    }

    /**
     * Same as {@link #testLocalBinning}, but this time via the 'gpt' command-line tool.
     *
     * @throws Exception if something goes badly wrong
     * @see #testLocalBinning()
     */
    @Test
    public void testLocalBinningViaGPT() throws Exception {

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final File parameterFile = new File(getClass().getResource("BinningParamsLocal.xml").toURI());
        final String fileName = "output.dim";
        final File targetFile = getTestFile(fileName);
        final File sourceFile1 = getTestFile("obs1.dim");
        final File sourceFile2 = getTestFile("obs2.dim");
        final File sourceFile3 = getTestFile("obs3.dim");
        final File sourceFile4 = getTestFile("obs4.dim");
        final File sourceFile5 = getTestFile("obs5.dim");

        ProductIO.writeProduct(createSourceProduct(1, obs1), sourceFile1, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(2, obs2), sourceFile2, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(3, obs3), sourceFile3, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(4, obs4), sourceFile4, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(5, obs5), sourceFile5, "BEAM-DIMAP", false);

        GPT.run(new String[]{
                "Binning",
                "-p", parameterFile.getPath(),
                "-t", targetFile.getPath(),
                sourceFile1.getPath(),
                sourceFile2.getPath(),
                sourceFile3.getPath(),
                sourceFile4.getPath(),
                sourceFile5.getPath(),
        });

        assertTrue(targetFile.exists());

        final Product targetProduct = ProductIO.readProduct(targetFile);
        assertNotNull(targetProduct);
        try {
            assertLocalBinningProductIsOk(targetProduct, targetFile, obs1, obs2, obs3, obs4, obs5);
        } finally {
            targetProduct.dispose();
        }
    }

    /**
     * Same as {@link #testGlobalBinning}, but this time via the 'gpt' command-line tool.
     *
     * @throws Exception if something goes badly wrong
     * @see #testLocalBinning()
     */
    @Test
    public void testGlobalBinningViaGPT_FilePattern() throws Exception {

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final File parameterFile = new File(getClass().getResource("BinningParamsGlobal_FilePattern.xml").toURI());
        final File targetFile = getTestFile("output.dim");
        final File sourceFile1 = getTestFile("obs1.dim");
        final File sourceFile2 = getTestFile("obs2.dim");
        final File sourceFile3 = getTestFile("obs3.dim");
        final File sourceFile4 = getTestFile("obs4.dim");
        final File sourceFile5 = getTestFile("obs5.dim");

        ProductIO.writeProduct(createSourceProduct(1, obs1), sourceFile1, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(2, obs2), sourceFile2, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(3, obs3), sourceFile3, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(4, obs4), sourceFile4, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(5, obs5), sourceFile5, "BEAM-DIMAP", false);

        GPT.run(new String[]{
                "Binning",
                "-p", parameterFile.getPath(),
                "-t", targetFile.getPath(),
        });

        assertTrue(targetFile.exists());

        final Product targetProduct = ProductIO.readProduct(targetFile);
        assertNotNull(targetProduct);
        try {
            assertGlobalBinningProductIsOk(targetProduct, targetFile, obs1, obs2, obs3, obs4, obs5);
        } finally {
            targetProduct.dispose();
        }
    }


    /**
     * Same as {@link #testLocalBinning}, but this time via the 'gpt' command-line tool.
     *
     * @throws Exception if something goes badly wrong
     * @see #testLocalBinning()
     */
    @Test
    public void testLocalBinningViaGPT_FilePattern() throws Exception {

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final File parameterFile = new File(getClass().getResource("BinningParamsLocal_FilePattern.xml").toURI());
        final String fileName = "output.dim";
        final File targetFile = getTestFile(fileName);
        final File sourceFile1 = getTestFile("obs1.dim");
        final File sourceFile2 = getTestFile("obs2.dim");
        final File sourceFile3 = getTestFile("obs3.dim");
        final File sourceFile4 = getTestFile("obs4.dim");
        final File sourceFile5 = getTestFile("obs5.dim");

        ProductIO.writeProduct(createSourceProduct(1, obs1), sourceFile1, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(2, obs2), sourceFile2, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(3, obs3), sourceFile3, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(4, obs4), sourceFile4, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(5, obs5), sourceFile5, "BEAM-DIMAP", false);

        GPT.run(new String[]{
                "Binning",
                "-p", parameterFile.getPath(),
                "-t", targetFile.getPath(),
        });

        assertTrue(targetFile.exists());

        final Product targetProduct = ProductIO.readProduct(targetFile);
        assertNotNull(targetProduct);
        try {
            assertLocalBinningProductIsOk(targetProduct, targetFile, obs1, obs2, obs3, obs4, obs5);
        } finally {
            targetProduct.dispose();
        }
    }

    @Test
    public void testCreateGeoCodingProductFilter() throws Exception {
        BinningOp binningOp = createBinningOp();
        binningOp.setTimeFilterMethod(BinningOp.TimeFilterMethod.NONE);

        final ProductFilter allProductsFilter = BinningOp.createSourceProductFilter(null, null, null, null);
        assertThat(allProductsFilter, is(instanceOf(GeoCodingProductFilter.class)));
    }

    @Test
    public void testCreateSpatialDataDayFilter() throws Exception {
        DataPeriod dataPeriod = TestUtils.createSpatialDataPeriod();

        Product product1 = TestUtils.createProduct(dataPeriod, DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.PREVIOUS_PERIODS);
        Product product2 = TestUtils.createProduct(dataPeriod, DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.CURRENT_PERIOD);
        Product product3 = TestUtils.createProduct(dataPeriod, DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.SUBSEQUENT_PERIODS);

        Product product4 = TestUtils.createProduct(dataPeriod, DataPeriod.Membership.CURRENT_PERIOD, DataPeriod.Membership.CURRENT_PERIOD);
        Product product5 = TestUtils.createProduct(dataPeriod, DataPeriod.Membership.CURRENT_PERIOD, DataPeriod.Membership.SUBSEQUENT_PERIODS);

        Product product6 = TestUtils.createProduct(dataPeriod, DataPeriod.Membership.SUBSEQUENT_PERIODS, DataPeriod.Membership.SUBSEQUENT_PERIODS);

        BinningOp binningOp = createBinningOp();
        binningOp.setTimeFilterMethod(BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY);
        ProductFilter filter = BinningOp.createSourceProductFilter(dataPeriod, null, null, null);

        assertSame(SpatialDataDaySourceProductFilter.class, filter.getClass());

        assertFalse(filter.accept(product1));
        assertTrue(filter.accept(product2));
        assertTrue(filter.accept(product3));
        assertTrue(filter.accept(product4));
        assertTrue(filter.accept(product5));
        assertFalse(filter.accept(product6));
    }

    @Test
    public void testParseStartDateUtc() {
        ProductData.UTC utc = BinningOp.parseStartDateUtc("2012-05-22");
        assertEquals("22-MAY-2012 00:00:00.000000", utc.format());
        utc = BinningOp.parseStartDateUtc("2012-05-22 11:22:33");
        assertEquals("22-MAY-2012 11:22:33.000000", utc.format());
    }

    @Test
    public void testParseDateUtc_errorCase() {
        try {
            BinningOp.parseStartDateUtc("yesterday evening");
            fail("OperatorException expected");
        } catch (OperatorException oe) {
            String expected = "Error while parsing start date parameter 'yesterday evening': Unparseable date: \"yesterday evening\"";
            assertEquals(expected, oe.getMessage());
        }
    }

    @Test
    public void testBinningSetsCorrectStartAndStopTimesFromProductTimes() throws Exception {
        final File targetFile = getTestFile("target-1.dim");
        float obs1 = 0.2F;

        final BinningOp binningOp = createBinningOp();

        final JtsGeometryConverter geometryConverter = new JtsGeometryConverter();

        final Product sourceProduct = createSourceProduct(1, obs1);
        sourceProduct.setStartTime(ProductData.UTC.parse("02-JAN-2002 11:30:25"));
        sourceProduct.setEndTime(ProductData.UTC.parse("02-JAN-2002 12:28:19"));

        binningOp.setSourceProducts(sourceProduct);
        binningOp.setAggregatorConfigs(chlAgg(), p70Agg());
        binningOp.setNumRows(180);
        binningOp.setMaskExpr("true");
        binningOp.setOutputFile(targetFile.getPath());
        binningOp.setOutputType("Product");
        binningOp.setOutputFormat("BEAM-DIMAP");
        binningOp.setRegion(geometryConverter.parse("POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))"));

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
        // TODO actually test something
        targetProduct.dispose();
    }

    @Test
    public void testCreateConfig() throws ParseException {
        final BinningOp binningOp = createBinningOp();
        binningOp.setNumRows(19);
        binningOp.setSuperSampling(20);
        binningOp.setMaskExpr("twenty-one");
        binningOp.setVariableConfigs(new VariableConfig("yeah", "wow"));
        binningOp.setAggregatorConfigs(new AggregatorMinMax.Config());
        binningOp.setPostProcessorConfig(new TestCellProcessorConfig());
        binningOp.setMinDataHour(25.0);
        binningOp.setMetadataAggregatorName("clump");
        binningOp.setStartDateTime("start-me-up");
        binningOp.setPeriodDuration(26.0);
        binningOp.setTimeFilterMethod(BinningOp.TimeFilterMethod.TIME_RANGE);
        binningOp.setOutputFile("a_file");
        final WKTReader wktReader = new WKTReader();
        binningOp.setRegion(wktReader.read("POLYGON((10 10, 15 10, 15 12, 10 12, 10 10))"));

        final BinningConfig config = binningOp.createConfig();
        assertNotNull(config);
        assertEquals(19, config.getNumRows());
        assertEquals(20, config.getSuperSampling().intValue());
        assertEquals("twenty-one", config.getMaskExpr());

        final VariableConfig[] variableConfigs = config.getVariableConfigs();
        assertEquals(1, variableConfigs.length);
        assertEquals("wow", variableConfigs[0].getExpr());

        final AggregatorConfig[] aggregatorConfigs = config.getAggregatorConfigs();
        assertEquals(1, aggregatorConfigs.length);
        assertEquals("MIN_MAX", aggregatorConfigs[0].getName());

        assertNotNull(config.getPostProcessorConfig());
        assertEquals(25.0, config.getMinDataHour(), 1e-8);
        assertEquals("clump", config.getMetadataAggregatorName());
        assertEquals("start-me-up", config.getStartDateTime());
        assertEquals(26.0, config.getPeriodDuration(), 1e-8);
        assertEquals(BinningOp.TimeFilterMethod.TIME_RANGE, config.getTimeFilterMethod());
        assertEquals("a_file", config.getOutputFile());
        assertEquals("POLYGON ((10 10, 15 10, 15 12, 10 12, 10 10))", config.getRegion().toString());
    }

    private BinningOp createBinningOp() {
        BinningOp binningOp = new BinningOp();
        binningOp.setParameterDefaultValues();
        return binningOp;
    }

    private void assertGlobalBinningProductIsOk(Product targetProduct, File location, float obs1, float obs2,
                                                float obs3, float obs4, float obs5) throws IOException {
        assertTargetProductIsOk(targetProduct, location, obs1, obs2, obs3, obs4, obs5, 360, 180, 179, 87);
    }

    private void assertLocalBinningProductIsOk(Product targetProduct, File location, float obs1, float obs2, float obs3,
                                               float obs4, float obs5) throws IOException {
        assertTargetProductIsOk(targetProduct, location, obs1, obs2, obs3, obs4, obs5, 4, 4, 0, 0);
    }

    private void assertTargetProductIsOk(Product targetProduct, File location, float obs1, float obs2, float obs3,
                                         float obs4, float obs5, int sceneRasterWidth, int sceneRasterHeight, int x0,
                                         int y0) throws IOException {
        final int w = 4;
        final int h = 4;

        final int _o_ = -1;
        final float _x_ = Float.NaN;

        assertEquals(location, targetProduct.getFileLocation());
        assertEquals(sceneRasterWidth, targetProduct.getSceneRasterWidth());
        assertEquals(sceneRasterHeight, targetProduct.getSceneRasterHeight());
        assertNotNull(targetProduct.getStartTime());
        assertNotNull(targetProduct.getEndTime());
        assertEquals("01-JAN-2002 00:00:00.000000", targetProduct.getStartTime().format());
        assertEquals("11-JAN-2002 00:00:00.000000", targetProduct.getEndTime().format());
        assertNotNull(targetProduct.getBand("num_obs"));
        assertEquals(ProductData.TYPE_INT32, targetProduct.getBand("num_obs").getDataType());
        assertNotNull(targetProduct.getBand("num_passes"));
        assertNotNull(targetProduct.getBand("chl_mean"));
        assertNotNull(targetProduct.getBand("chl_sigma"));
        assertNotNull(targetProduct.getBand("chl_p70"));
        assertEquals(_o_, targetProduct.getBand("num_obs").getNoDataValue(), 1e-10);
        assertEquals(_o_, targetProduct.getBand("num_passes").getNoDataValue(), 1e-10);
        assertEquals(_x_, targetProduct.getBand("chl_mean").getNoDataValue(), 1e-10);
        assertEquals(_x_, targetProduct.getBand("chl_sigma").getNoDataValue(), 1e-10);
        assertEquals(_x_, targetProduct.getBand("chl_p70").getNoDataValue(), 1e-10);

        // Test pixel values of band "num_obs"
        //
        final int nob = 5;
        final int[] expectedNobs = new int[]{
                _o_, _o_, _o_, _o_,
                _o_, nob, nob, _o_,
                _o_, nob, nob, _o_,
                _o_, _o_, _o_, _o_,
        };
        final int[] actualNobs = new int[w * h];
        targetProduct.getBand("num_obs").getSourceImage().getData().getPixels(x0, y0, w, h, actualNobs);
        assertArrayEquals(expectedNobs, actualNobs);

        // Test pixel values of band "num_passes"
        //
        final int npa = 5;
        final int[] expectedNpas = new int[]{
                _o_, _o_, _o_, _o_,
                _o_, npa, npa, _o_,
                _o_, npa, npa, _o_,
                _o_, _o_, _o_, _o_,
        };
        final int[] actualNpas = new int[w * h];
        targetProduct.getBand("num_passes").getSourceImage().getData().getPixels(x0, y0, w, h, actualNpas);
        assertArrayEquals(expectedNpas, actualNpas);

        // Test pixel values of band "chl_mean"
        //
        final float mea = (obs1 + obs2 + obs3 + obs4 + obs5) / nob;
        final float[] expectedMeas = new float[]{
                _x_, _x_, _x_, _x_,
                _x_, mea, mea, _x_,
                _x_, mea, mea, _x_,
                _x_, _x_, _x_, _x_,
        };
        final float[] actualMeas = new float[w * h];
        targetProduct.getBand("chl_mean").getSourceImage().getData().getPixels(x0, y0, w, h, actualMeas);
        assertArrayEquals(expectedMeas, actualMeas, 1e-4F);

        // Test pixel values of band "chl_sigma"
        //
        final float sig = (float) sqrt(
                (obs1 * obs1 + obs2 * obs2 + obs3 * obs3 + obs4 * obs4 + obs5 * obs5) / nob - mea * mea);
        final float[] expectedSigs = new float[]{
                _x_, _x_, _x_, _x_,
                _x_, sig, sig, _x_,
                _x_, sig, sig, _x_,
                _x_, _x_, _x_, _x_,
        };
        final float[] actualSigs = new float[w * h];
        targetProduct.getBand("chl_sigma").getSourceImage().getData().getPixels(x0, y0, w, h, actualSigs);
        assertArrayEquals(expectedSigs, actualSigs, 1e-4F);

        // Test pixel values of band "chl_p70"
        //
        final float p70 = AggregatorPercentile.computePercentile(70, new float[]{obs1, obs2, obs3, obs4, obs5});
        final float[] expectedP70 = new float[]{
                _x_, _x_, _x_, _x_,
                _x_, p70, p70, _x_,
                _x_, p70, p70, _x_,
                _x_, _x_, _x_, _x_,
        };
        final float[] actualP70 = new float[w * h];
        targetProduct.getBand("chl_p70").getSourceImage().getData().getPixels(x0, y0, w, h, actualP70);
        assertArrayEquals(expectedP70, actualP70, 1e-4F);
    }

    private static AggregatorPercentile.Config p70Agg() {
        return new AggregatorPercentile.Config(null, "chl", 70);
    }

    private static AggregatorAverage.Config chlAgg() {
        return new AggregatorAverage.Config("chl");
    }

    static Product createSourceProduct(int sourceProductCounter, float value) {
        final Product p = new Product("P" + sourceProductCounter, "T", 2, 2);
        final TiePointGrid latitude = new TiePointGrid("latitude", 2, 2, 0.5F, 0.5F, 1.0F, 1.0F, new float[]{
                1.0F, 1.0F,
                0.0F, 0.0F,
        });
        final TiePointGrid longitude = new TiePointGrid("longitude", 2, 2, 0.5F, 0.5F, 1.0F, 1.0F, new float[]{
                0.0F, 1.0F,
                0.0F, 1.0F,
        });
        p.addTiePointGrid(latitude);
        p.addTiePointGrid(longitude);
        p.setGeoCoding(new TiePointGeoCoding(latitude, longitude));
        p.addBand("chl", value + "");
        return p;
    }

    static File getTestFile(String fileName) {
        return new File(TESTDATA_DIR, fileName);
    }

    private static class TestCellProcessorConfig extends CellProcessorConfig {

    }
}
