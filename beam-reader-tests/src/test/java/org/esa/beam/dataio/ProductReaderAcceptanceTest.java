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

package org.esa.beam.dataio;


import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.support.DefaultViewport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.SystemUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.*;

import static org.junit.Assert.*;

@RunWith(ReaderTestRunner.class)
public class ProductReaderAcceptanceTest {

    private static final String PROPERTYNAME_DATA_DIR = "beam.reader.tests.data.dir";
    private static final String PROPERTYNAME_FAIL_ON_MISSING_DATA = "beam.reader.tests.failOnMissingData";
    private static final String PROPERTYNAME_FAIL_ON_INTENDED = "beam.reader.tests.failOnMultipleIntendedReaders";
    private static final String PROPERTYNAME_LOG_FILE_PATH = "beam.reader.tests.log.file";
    private static final String PROPERTYNAME_CASS_NAME = "beam.reader.tests.class.name";
    private static final boolean FAIL_ON_MISSING_DATA = Boolean.parseBoolean(System.getProperty(PROPERTYNAME_FAIL_ON_MISSING_DATA, "true"));
    private static final String INDENT = "\t";
    private static final ProductList testProductList = new ProductList();
    private static final int DECODE_QUALI_LOG_THRESHOLD = 50;
    private static TestDefinitionList testDefinitionList;
    private static File dataRootDir;
    private static Logger logger;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH);
    private static final Calendar CALENDAR = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);

    @BeforeClass
    public static void initialize() throws Exception {
        initLogger();

        logFailOnMissingDataMessage();

        assertTestDataDirectory();
        loadProductReaderTestDefinitions();

        createGlobalProductList();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        logInfoWithStars("Finished / " + DATE_FORMAT.format(CALENDAR.getTime()));
    }

    @Test
    public void testOneIntendedReader() {
        logInfoWithStars("Testing OneIntendedReader");
        boolean duplicates = false;
        for (TestProduct testProduct : testProductList) {
            if (testProduct.exists()) {
                List<ProductReaderPlugIn> intendedPlugins = new ArrayList<>();
                for (TestDefinition testDefinition : testDefinitionList) {
                    if (DecodeQualification.INTENDED == getExpectedDecodeQualification(testDefinition, testProduct)) {
                        intendedPlugins.add(testDefinition.getProductReaderPlugin());
                    }
                }
                if (intendedPlugins.size() > 1) {
                    logger.info(INDENT + testProduct.getId());
                    for (ProductReaderPlugIn intendedPlugin : intendedPlugins) {
                        logger.info(INDENT + INDENT + intendedPlugin.getClass().getName());
                    }
                    duplicates = true;
                }
            }
        }
        if (duplicates && Boolean.parseBoolean(System.getProperty(PROPERTYNAME_FAIL_ON_INTENDED, "false"))) {
            fail("Products are accepted by more than one ReaderPlugin as 'INTENDED'");
        }
    }

    @Test
    public void testPluginDecodeQualifications() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        logInfoWithStars("Testing DecodeQualification");
        final StopWatch stopWatchTotal = new StopWatch();
        stopWatchTotal.start();
        int testCounter = 0;
        final StopWatch stopWatch = new StopWatch();

        logger.info("");
        logger.info(INDENT + "Number of test products: " + testProductList.size());
        logger.info(INDENT + "Number of ReaderPlugIns: " + testDefinitionList.size());
        logger.info(INDENT + "Logging only decode qualification tests >" + DECODE_QUALI_LOG_THRESHOLD + "ms");
        logger.info("");

        for (TestDefinition testDefinition : testDefinitionList) {
            final ProductReaderPlugIn productReaderPlugin = testDefinition.getProductReaderPlugin();
            logger.info(INDENT + productReaderPlugin.getClass().getName());

            for (TestProduct testProduct : testProductList) {
                if (testProduct.exists()) {
                    final File productFile = getTestProductFile(testProduct);

                    final DecodeQualification expected = getExpectedDecodeQualification(testDefinition, testProduct);
                    stopWatch.start();
                    final DecodeQualification decodeQualification = productReaderPlugin.getDecodeQualification(productFile);
                    stopWatch.stop();

                    final String message = productReaderPlugin.getClass().getName() + ": " + testProduct.getId();
                    assertEquals(message, expected, decodeQualification);
                    if (stopWatch.getTimeDiff() > DECODE_QUALI_LOG_THRESHOLD) {
                        logger.info(INDENT + INDENT + stopWatch.getTimeDiffString() + " - [" + expected + "] " + testProduct.getId());
                    }
                    testCounter++;
                } else {
                    logProductNotExistent(2, testProduct);
                }
            }
        }
        stopWatchTotal.stop();
        logInfoWithStars(String.format("Tested DecodeQualification: %d tests in %s", testCounter, stopWatchTotal.getTimeDiffString()));
    }

    @Test
    public void testReadIntendedProductContent() throws IOException {
        logInfoWithStars("Testing IntendedProductContent");
        final StopWatch stopWatchTotal = new StopWatch();
        stopWatchTotal.start();
        int testCounter = 0;
        final StopWatch stopWatch = new StopWatch();

        for (TestDefinition testDefinition : testDefinitionList) {
            final List<String> intendedProductIds = testDefinition.getDecodableProductIds();
            logger.info(INDENT + testDefinition.getProductReaderPlugin().getClass().getSimpleName());

            for (String productId : intendedProductIds) {
                final TestProduct testProduct = testProductList.getById(productId);
                assertNotNull("Test file not defined for ID=" + productId, testProduct);

                if (testProduct.exists()) {
                    final File testProductFile = getTestProductFile(testProduct);

                    final ProductReader productReader = testDefinition.getProductReaderPlugin().createReaderInstance();
                    stopWatch.start();
                    final Product product = productReader.readProductNodes(testProductFile, null);
                    try {
                        assertExpectedContent(testDefinition, productId, product);
                    } finally {
                        if (product != null) {
                            product.dispose();
                        }
                    }
                    stopWatch.stop();
                    logger.info(INDENT + INDENT + stopWatch.getTimeDiffString() + " - " + testProduct.getId());
                    testCounter++;
                } else {
                    logProductNotExistent(2, testProduct);
                }
            }
        }
        stopWatchTotal.stop();
        logInfoWithStars(String.format("Tested IntendedProductContent: %d tests in %s", testCounter, stopWatchTotal.getTimeDiffString()));
    }

    @Test
    public void testProductIO_readProduct() throws Exception {
        logInfoWithStars("Testing ProductIO.readProduct");
        final StopWatch stopWatchTotal = new StopWatch();
        stopWatchTotal.start();
        int testCounter = 0;
        final StopWatch stopWatch = new StopWatch();
        for (TestProduct testProduct : testProductList) {
            if (testProduct.exists()) {
                final File testProductFile = getTestProductFile(testProduct);
                Product product = null;
                try {
                    stopWatch.start();
                    product = ProductIO.readProduct(testProductFile);
                    stopWatch.stop();
                    logger.info(INDENT + stopWatch.getTimeDiffString() + " - " + testProduct.getId());
                } catch (Exception e) {
                    final String message = "ProductIO.readProduct " + testProduct.getId() + " caused an exception.\n" +
                            "Should only return NULL or a product instance but should not cause any exception.";
                    logger.log(Level.SEVERE, message, e);
                    fail(message);
                } finally {
                    if (product != null) {
                        product.dispose();
                    }
                }
                testCounter++;
            } else {
                logProductNotExistent(1, testProduct);
            }
        }
        stopWatchTotal.stop();
        logInfoWithStars(String.format("Tested ProductIO.readProduct: %d tests in %s", testCounter, stopWatchTotal.getTimeDiffString()));
    }

    @Test
    public void testProductReadTimes() throws Exception {
        logInfoWithStars("Testing product read times");
        logger.info(String.format("%s%s - %s - %s - %s", INDENT,
                " findReader ",
                " readNodes  ",
                "   getStx   ",
                " getViewData"));
        final StopWatch stopWatchTotal = new StopWatch();
        stopWatchTotal.start();
        int testCounter = 0;
        final StopWatch stopWatch = new StopWatch();
        for (TestProduct testProduct : testProductList) {
            if (testProduct.exists()) {
                final File testProductFile = getTestProductFile(testProduct);
                Product product = null;
                try {
                    stopWatch.start();

                    //product = ProductIO.readProduct(testProductFile);
                    // method inlined for detailed time measuring
                    final ProductReader productReader = ProductIO.getProductReaderForInput(testProductFile);
                    stopWatch.stop();
                    String findProductReaderTime = stopWatch.getTimeDiffString();

                    String readProductNodesTime = "--:--:--.---";
                    String getStxTime = "--:--:--.---";
                    String getViewDataTime = "--:--:--.---";
                    if (productReader != null) {
                        stopWatch.start();
                        product = productReader.readProductNodes(testProductFile, null);
                        stopWatch.stop();
                        readProductNodesTime = stopWatch.getTimeDiffString();
                        if (product.getNumBands() > 0) {
                            Band band0 = product.getBandAt(0);
                            stopWatch.start();
                            Stx stx = band0.getStx();
                            assertNotNull(stx);
                            stopWatch.stop();
                            getStxTime = stopWatch.getTimeDiffString();
                            DefaultViewport viewport = new DefaultViewport(new Rectangle(1000, 1000));
                            int viewLevel = ImageLayer.getLevel(band0.getSourceImage().getModel(), viewport);
                            RenderedImage viewImage = band0.getSourceImage().getImage(viewLevel);

                            stopWatch.start();
                            final int numXTiles = viewImage.getNumXTiles();
                            final int numYTiles = viewImage.getNumYTiles();
                            if (numXTiles > 0 && numYTiles > 0) {
                                for (int x = 0; x < numXTiles; x++) {
                                    for (int y = 0; y < numYTiles; y++) {
                                        Raster tileRaster = viewImage.getTile(x, y);
                                        assertNotNull("tileRaster", tileRaster);
                                    }
                                }
                            } else {
                                Raster imageRaster = viewImage.getData();
                                assertNotNull("imageRaster", imageRaster);
                            }
                            stopWatch.stop();
                            getViewDataTime = stopWatch.getTimeDiffString();
                        }
                    }
                    logger.info(String.format("%s%s - %s - %s - %s - %s", INDENT,
                            findProductReaderTime,
                            readProductNodesTime,
                            getStxTime,
                            getViewDataTime,
                            testProduct.getId()));
                } catch (Exception e) {
                    final String message = "Product reading " + testProduct.getId() + " caused an exception.";
                    logger.log(Level.SEVERE, message, e);
                    fail(message);
                } finally {
                    if (product != null) {
                        product.dispose();
                    }
                }
                testCounter++;
            } else {
                logProductNotExistent(1, testProduct);
            }
        }
        stopWatchTotal.stop();
        logInfoWithStars(String.format("Testing product read times: %d tests in %s", testCounter, stopWatchTotal.getTimeDiffString()));
    }

    private static void assertExpectedContent(TestDefinition testDefinition, String productId, Product product) {
        final ExpectedContent expectedContent = testDefinition.getExpectedContent(productId);
        if (expectedContent == null) {
            return;
        }

        final ContentAssert contentAssert = new ContentAssert(expectedContent, productId, product);
        contentAssert.assertProductContent();
    }

    private static DecodeQualification getExpectedDecodeQualification(TestDefinition testDefinition, TestProduct testProduct) {
        final ExpectedDataset expectedDataset = testDefinition.getExpectedDataset(testProduct.getId());
        if (expectedDataset != null) {
            return expectedDataset.getDecodeQualification();
        }
        return DecodeQualification.UNABLE;
    }

    private static File getTestProductFile(TestProduct testProduct) {
        final String relativePath = testProduct.getRelativePath();
        final File testProductFile = new File(dataRootDir, relativePath);
        assertTrue(testProductFile.exists());
        return testProductFile;
    }

    private static void logProductNotExistent(int indention, TestProduct testProduct) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indention; i++) {
            sb.append(INDENT);
        }
        logger.info(sb.toString() + "Not existent - " + testProduct.getId());
    }

    private static void logFailOnMissingDataMessage() {
        if (!FAIL_ON_MISSING_DATA) {
            logger.warning("Tests will not fail if test data is missing!");
        }
    }

    private static void assertTestDataDirectory() {
        final String dataDirProperty = System.getProperty(PROPERTYNAME_DATA_DIR);
        if (dataDirProperty == null) {
            fail("Data directory path not set");
        }
        dataRootDir = new File(dataDirProperty);
        if (!dataRootDir.isDirectory()) {
            fail("Data directory is not valid: " + dataDirProperty);
        }
    }

    private static void initLogger() throws Exception {
        // Suppress ugly (and harmless) JAI error messages saying that a JAI is going to continue in pure Java mode.
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");  // disable native libraries for JAI

        logger = Logger.getLogger(ProductReaderAcceptanceTest.class.getSimpleName());
        removeRootLogHandler();
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CustomLogFormatter());
        logger.addHandler(consoleHandler);
        final String logFilePath = System.getProperty(PROPERTYNAME_LOG_FILE_PATH);
        if (logFilePath != null) {
            final File logFile = new File(logFilePath);
            final FileOutputStream fos = new FileOutputStream(logFile);
            final StreamHandler streamHandler = new StreamHandler(fos, new CustomLogFormatter());
            logger.addHandler(streamHandler);
        }
        logInfoWithStars("Reader Acceptance Tests / " + DATE_FORMAT.format(CALENDAR.getTime()));
    }

    private static void removeRootLogHandler() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }
    }

    private static void createGlobalProductList() {
        for (TestDefinition testDefinition : testDefinitionList) {
            final List<TestProduct> allPluginProducts = testDefinition.getAllProducts();
            for (TestProduct testProduct : allPluginProducts) {
                if (!testIfIdAlreadyRegistered(testProduct)) {
                    testProductList.add(testProduct);
                }
            }
        }
    }

    private static boolean testIfIdAlreadyRegistered(TestProduct testProduct) {
        final String id = testProduct.getId();
        final TestProduct storedProduct = testProductList.getById(id);
        if (storedProduct != null) {
            if (storedProduct.isDifferent(testProduct)) {
                fail("Test file with ID=" + id + " already defined with different settings");
            }

            return true;
        }

        return false;
    }

    private static void testIfProductFilesExists(ProductList productList) {
        for (TestProduct testProduct : productList) {
            final String relativePath = testProduct.getRelativePath();
            final File productFile = new File(dataRootDir, relativePath);
            if (!productFile.exists()) {
                testProduct.exists(false);
                if (FAIL_ON_MISSING_DATA) {
                    fail("Test product does not exist: " + productFile.getAbsolutePath());
                }
            }
        }
    }

    private static void loadProductReaderTestDefinitions() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Iterable<ProductReaderPlugIn> readerPlugIns = SystemUtils.loadServices(ProductReaderPlugIn.class);
        testDefinitionList = new TestDefinitionList();

        final String className = System.getProperty(PROPERTYNAME_CASS_NAME);

        for (ProductReaderPlugIn readerPlugIn : readerPlugIns) {
            final Class<? extends ProductReaderPlugIn> readerPlugInClass = readerPlugIn.getClass();
            if (className != null && !readerPlugInClass.getName().equals(className)) {
                continue;
            }

            final String dataResourceName = getReaderTestResourceName(readerPlugInClass.getName(), "-data.json");
            final URL dataResource = readerPlugInClass.getResource(dataResourceName);
            if (dataResource == null) {
                logger.warning(readerPlugInClass.getSimpleName() + " does not define test data");
                continue;
            }

            final TestDefinition testDefinition = new TestDefinition();
            testDefinition.setProductReaderPlugin(readerPlugIn);
            testDefinitionList.add(testDefinition);

            final ProductList productList = mapper.readValue(dataResource, ProductList.class);
            testIfProductFilesExists(productList);
            testDefinition.addTestProducts(productList.getAll());

            final String[] ids = productList.getAllIds();
            for (String id : ids) {
                final String fileResourceName = id + ".json";
                final URL fileResource = readerPlugInClass.getResource(fileResourceName);
                if (fileResource == null) {
                    fail(readerPlugInClass.getSimpleName() + " resource file '" + fileResourceName + "' is missing");
                }

                final ExpectedDataset expectedDataset = mapper.readValue(fileResource, ExpectedDataset.class);
                testDefinition.addExpectedDataset(expectedDataset);
            }
        }
    }

    private static String getReaderTestResourceName(String fullyQualifiedName, String suffix) {
        final String path = fullyQualifiedName.replace(".", "/");
        return "/" + path + suffix;
    }

    private static void logInfoWithStars(final String text) {
        final String msg = "  " + text + "  ";
        final char[] stars = new char[msg.length()];
        Arrays.fill(stars, '*');
        final String starString = new String(stars);
        logger.info("");
        logger.info(starString);
        logger.info(msg);
        logger.info(starString);
        logger.info("");
    }
}
