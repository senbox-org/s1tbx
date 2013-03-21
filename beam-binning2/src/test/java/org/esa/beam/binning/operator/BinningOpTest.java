package org.esa.beam.binning.operator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.main.GPT;
import org.esa.beam.util.converters.JtsGeometryConverter;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;

import static java.lang.Math.*;
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
        if (!TESTDATA_DIR.isDirectory()) {
            fail("Can't create test I/O directory: " + TESTDATA_DIR);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (!FileUtils.deleteTree(TESTDATA_DIR)) {
            System.out.println("Warning: failed to completely delete test I/O directory:" + TESTDATA_DIR);
        }
    }

    @Test
    public void testMetadataGeneration() throws Exception {
        BinningOp binningOp = new BinningOp();

        binningOp.setSourceProducts(createSourceProduct(0.1F),
                                    createSourceProduct(0.2F),
                                    createSourceProduct(0.3F));

        binningOp.setStartDate("2002-01-01");
        binningOp.setEndDate("2002-01-10");
        binningOp.setBinningConfig(createBinningConfig());
        binningOp.setFormatterConfig(createFormatterConfig());

        binningOp.setParameter("metadataTemplateDir", TESTDATA_DIR);

        assertNull(binningOp.getMetadataProperties());

        binningOp.getTargetProduct();

        SortedMap<String, String> metadataProperties = binningOp.getMetadataProperties();
        assertNotNull(metadataProperties);
        Set<String> nameSet = metadataProperties.keySet();
        final String[] names = nameSet.toArray(new String[nameSet.size()]);
        assertEquals(5, names.length);
        assertEquals("processing_time", names[0]);
        assertEquals("product_name", names[1]);
        assertEquals("software_name", names[2]);
        assertEquals("software_qualified_name", names[3]);
        assertEquals("software_version", names[4]);
    }

    @Test
    public void testInvalidDates() throws Exception {
        final BinningOp binningOp = new BinningOp();
        binningOp.setStartDate("2010-01-01");
        binningOp.setEndDate("2009-01-01");
        try {
            binningOp.initialize();
            fail();
        } catch (OperatorException expected) {
            assertTrue(expected.getMessage().contains("before"));
        }
    }

    @Test
    public void testBinningWithEmptyMaskExpression() throws Exception {

        BinningConfig binningConfig = createBinningConfig();
        binningConfig.setMaskExpr("");
        FormatterConfig formatterConfig = createFormatterConfig();

        float obs1 = 0.2F;

        final BinningOp binningOp = new BinningOp();

        JtsGeometryConverter geometryConverter = new JtsGeometryConverter();
        binningOp.setSourceProducts(createSourceProduct(obs1));
        binningOp.setStartDate("2002-01-01");
        binningOp.setEndDate("2002-01-10");
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);
        binningOp.setRegion(geometryConverter.parse("POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))"));

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
    }

    @Test
    public void testBinningWhenMaskExpressionIsNull() throws Exception {

        BinningConfig binningConfig = createBinningConfig();
        binningConfig.setMaskExpr(null);
        FormatterConfig formatterConfig = createFormatterConfig();

        float obs1 = 0.2F;

        final BinningOp binningOp = new BinningOp();

        JtsGeometryConverter geometryConverter = new JtsGeometryConverter();
        binningOp.setSourceProducts(createSourceProduct(obs1));
        binningOp.setStartDate("2002-01-01");
        binningOp.setEndDate("2002-01-10");
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);
        binningOp.setRegion(geometryConverter.parse("POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))"));

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
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

        BinningConfig binningConfig = createBinningConfig();
        FormatterConfig formatterConfig = createFormatterConfig();

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final BinningOp binningOp = new BinningOp();

        binningOp.setSourceProducts(createSourceProduct(obs1),
                                    createSourceProduct(obs2),
                                    createSourceProduct(obs3),
                                    createSourceProduct(obs4),
                                    createSourceProduct(obs5));

        JtsGeometryConverter geometryConverter = new JtsGeometryConverter();
        binningOp.setStartDate("2002-01-01");
        binningOp.setEndDate("2002-01-10");
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);
        binningOp.setRegion(geometryConverter.parse("POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))"));

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
        try {
            assertGlobalBinningProductIsOk(targetProduct, null, obs1, obs2, obs3, obs4, obs5);
        } catch (Exception e) {
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

        BinningConfig binningConfig = createBinningConfig();
        FormatterConfig formatterConfig = createFormatterConfig();

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final BinningOp binningOp = new BinningOp();

        binningOp.setSourceProducts(createSourceProduct(obs1),
                                    createSourceProduct(obs2),
                                    createSourceProduct(obs3),
                                    createSourceProduct(obs4),
                                    createSourceProduct(obs5));

        GeometryFactory gf = new GeometryFactory();
        binningOp.setRegion(gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                new Coordinate(-1.0, -1.0),
                new Coordinate(3.0, -1.0),
                new Coordinate(3.0, 3.0),
                new Coordinate(-1.0, 3.0),
                new Coordinate(-1.0, -1.0),
        }), null));
        binningOp.setStartDate("2002-01-01");
        binningOp.setEndDate("2002-01-10");
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);

        final Product targetProduct = binningOp.getTargetProduct();
        assertNotNull(targetProduct);
        try {
            assertLocalBinningProductIsOk(targetProduct, null, obs1, obs2, obs3, obs4, obs5);
        } catch (IOException e) {
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

        BinningConfig binningConfig = createBinningConfig();
        FormatterConfig formatterConfig = createFormatterConfig();

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("startDate", "2002-01-01");
        parameters.put("endDate", "2002-01-10");
        parameters.put("binningConfig", binningConfig);
        parameters.put("formatterConfig", formatterConfig);
        parameters.put("region", "POLYGON ((-180 -90, -180 90, 180 90, 180 -90, -180 -90))");

        final Product targetProduct = GPF.createProduct("Binning", parameters,
                                                        createSourceProduct(obs1),
                                                        createSourceProduct(obs2),
                                                        createSourceProduct(obs3),
                                                        createSourceProduct(obs4),
                                                        createSourceProduct(obs5));

        assertNotNull(targetProduct);
        try {
            assertGlobalBinningProductIsOk(targetProduct, null, obs1, obs2, obs3, obs4, obs5);
        } catch (Exception e) {
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

        BinningConfig binningConfig = createBinningConfig();
        FormatterConfig formatterConfig = createFormatterConfig();

        float obs1 = 0.2F;
        float obs2 = 0.4F;
        float obs3 = 0.6F;
        float obs4 = 0.8F;
        float obs5 = 1.0F;

        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("region", "POLYGON((-1 -1, 3 -1, 3 3, -1 3, -1 -1))");
        parameters.put("startDate", "2002-01-01");
        parameters.put("endDate", "2002-01-10");
        parameters.put("binningConfig", binningConfig);
        parameters.put("formatterConfig", formatterConfig);

        final Product targetProduct = GPF.createProduct("Binning",
                                                        parameters,
                                                        createSourceProduct(obs1),
                                                        createSourceProduct(obs2),
                                                        createSourceProduct(obs3),
                                                        createSourceProduct(obs4),
                                                        createSourceProduct(obs5));
        assertNotNull(targetProduct);
        try {
            assertLocalBinningProductIsOk(targetProduct, null, obs1, obs2, obs3, obs4, obs5);
        } catch (IOException e) {
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

        ProductIO.writeProduct(createSourceProduct(obs1), sourceFile1, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs2), sourceFile2, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs3), sourceFile3, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs4), sourceFile4, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs5), sourceFile5, "BEAM-DIMAP", false);

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
        } catch (IOException e) {
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

        ProductIO.writeProduct(createSourceProduct(obs1), sourceFile1, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs2), sourceFile2, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs3), sourceFile3, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs4), sourceFile4, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs5), sourceFile5, "BEAM-DIMAP", false);

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
        } catch (IOException e) {
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

        ProductIO.writeProduct(createSourceProduct(obs1), sourceFile1, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs2), sourceFile2, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs3), sourceFile3, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs4), sourceFile4, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs5), sourceFile5, "BEAM-DIMAP", false);

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
        } catch (IOException e) {
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

        ProductIO.writeProduct(createSourceProduct(obs1), sourceFile1, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs2), sourceFile2, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs3), sourceFile3, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs4), sourceFile4, "BEAM-DIMAP", false);
        ProductIO.writeProduct(createSourceProduct(obs5), sourceFile5, "BEAM-DIMAP", false);

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
        } catch (IOException e) {
            targetProduct.dispose();
        }
    }

    @Test
    public void testFilterAccordingToTime() throws Exception {
        GeoCoding gcMock = Mockito.mock(GeoCoding.class);
        Mockito.when(gcMock.canGetGeoPos()).thenReturn(true);
        final Product product1 = new Product("name1", "type", 10, 10);
        product1.setGeoCoding(gcMock);
        final Product product2 = new Product("name2", "type", 10, 10);
        product2.setGeoCoding(gcMock);
        final Product product3 = new Product("name3", "type", 10, 10);
        product3.setGeoCoding(gcMock);
        Product[] inputProducts = {product1, product2, product3};
        Product[] expectedProducts = {product1, product2, product3};
        Product[] filteredProducts = BinningOp.filterSourceProducts(inputProducts,
                                                                    ProductData.UTC.parse("01-JUL-2000 00:00:00"),
                                                                    ProductData.UTC.parse("01-AUG-2000 00:00:00"));
        assertArrayEquals(expectedProducts, filteredProducts);

        product1.setStartTime(ProductData.UTC.parse("02-JUL-2000 00:00:00"));
        product1.setEndTime(ProductData.UTC.parse("02-AUG-2000 00:00:00"));

        inputProducts = new Product[]{product1, product2, product3};
        expectedProducts = new Product[]{product2, product3};
        filteredProducts = BinningOp.filterSourceProducts(inputProducts, ProductData.UTC.parse("01-JUL-2000 00:00:00"),
                                                          ProductData.UTC.parse("01-AUG-2000 00:00:00"));
        assertArrayEquals(expectedProducts, filteredProducts);
    }

    @Test
    public void testSetRegionToProductsExtent() throws Exception {
        BinningOp binningOp = new BinningOp();
        final Product product1 = new Product("name1", "type", 10, 10);
        final Product product2 = new Product("name2", "type", 10, 10);

        product1.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 10, 10, 10.0, 50.0, 1.0, 1.0));
        product2.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 10, 10, 15.0, 45.0, 1.0, 1.0));

        binningOp.sourceProducts = new Product[]{product1, product2};
        binningOp.setRegionToProductsExtent();
        Geometry region = binningOp.getRegion();

        GeneralPath shape = new GeneralPath();
        shape.moveTo((float) region.getCoordinates()[0].x, (float) region.getCoordinates()[0].y);

        for (int i = 1; i < region.getNumPoints(); i++) {
            shape.lineTo((float) region.getCoordinates()[i].x, (float) region.getCoordinates()[i].y);
        }

        Rectangle2D.Double expected = new Rectangle2D.Double(10.0, 36.0, 14.0, 14.0);

        assertEquals(expected, shape.getBounds2D());
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
        assertEquals("10-JAN-2002 00:00:00.000000", targetProduct.getEndTime().format());
        assertNotNull(targetProduct.getBand("num_obs"));
        assertEquals(ProductData.TYPE_INT32, targetProduct.getBand("num_obs").getDataType());
        assertNotNull(targetProduct.getBand("num_passes"));
        assertNotNull(targetProduct.getBand("chl_mean"));
        assertNotNull(targetProduct.getBand("chl_sigma"));
        assertEquals(_o_, targetProduct.getBand("num_obs").getNoDataValue(), 1e-10);
        assertEquals(_o_, targetProduct.getBand("num_passes").getNoDataValue(), 1e-10);
        assertEquals(_x_, targetProduct.getBand("chl_mean").getNoDataValue(), 1e-10);
        assertEquals(_x_, targetProduct.getBand("chl_sigma").getNoDataValue(), 1e-10);

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
        targetProduct.getBand("num_obs").readPixels(x0, y0, w, h, actualNobs);
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
        targetProduct.getBand("num_passes").readPixels(x0, y0, w, h, actualNpas);
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
        targetProduct.getBand("chl_mean").readPixels(x0, y0, w, h, actualMeas);
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
        targetProduct.getBand("chl_sigma").readPixels(x0, y0, w, h, actualSigs);
        assertArrayEquals(expectedSigs, actualSigs, 1e-4F);
    }

    static BinningConfig createBinningConfig() {
        AggregatorAverage.Config c = new AggregatorAverage.Config();
        c.setVarName("chl");
        final BinningConfig binningConfig = new BinningConfig();
        binningConfig.setAggregatorConfigs(c);
        binningConfig.setNumRows(180);
        binningConfig.setMaskExpr("true");
        return binningConfig;
    }

    static int sourceProductCounter = 1;
    static int targetProductCounter = 1;

    static FormatterConfig createFormatterConfig() throws IOException {
        final File targetFile = getTestFile("target-" + (targetProductCounter++) + ".dim");
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFile(targetFile.getPath());
        formatterConfig.setOutputType("Product");
        formatterConfig.setOutputFormat("BEAM-DIMAP");
        return formatterConfig;
    }

    static Product createSourceProduct() {
        return createSourceProduct(1.0F);
    }

    static Product createSourceProduct(float value) {
        final Product p = new Product("P" + sourceProductCounter++, "T", 2, 2);
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

}
