package org.esa.beam.dataio;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

@RunWith(ReaderTestRunner.class)
public class ProductReaderAcceptanceTest {

    private static final String PROPERTYNAME_DATA_DIR = "beam.reader.tests.data.dir";
    private static final String PROPERTYNAME_FAIL_ON_MISSING_DATA = "beam.reader.tests.failOnMissingData";
    private static final boolean FAIL_ON_MISSING_DATA = Boolean.parseBoolean(System.getProperty(PROPERTYNAME_FAIL_ON_MISSING_DATA, "true"));
    private static final String INDENT = "\t";
    private static ProductList testProductList;
    private static ProductReaderList productReaderList;
    private static File dataRootDir;
    private static Logger logger;

    @BeforeClass
    public static void initialize() throws IOException {
        initLogger();
        if (!FAIL_ON_MISSING_DATA) {
            logger.warning("Tests will not fail if test data is missing!");
        }
        readTestDataDirProperty();

        readProductsList();
        validateProductList();

        readProductReadersList();
    }

    private static void initLogger() throws IOException {
        logger = Logger.getLogger(ProductReaderAcceptanceTest.class.getSimpleName());
        BeamLogManager.removeRootLoggerHandlers();
        final ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new CustomLogFormatter());
        logger.addHandler(handler);
    }


    @Test
    public void testPluginDecodeQualifications() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final ArrayList<TestProductReader> testReaders = productReaderList.getTestReaders();
        logger.info("Testing DecodeQualification:");
        final StopWatch stopWatch = new StopWatch();
        for (TestProductReader testReader : testReaders) {
            final ProductReaderPlugIn productReaderPlugin = testReader.getProductReaderPlugin();
            logger.info(INDENT + productReaderPlugin.getClass().getSimpleName());

            for (TestProduct testProduct : testProductList) {
                if (testProduct.exists()) {
                    final File productFile = getTestProductFile(testProduct);

                    final DecodeQualification expected = getExpectedDecodeQualification(testReader, testProduct);
                    stopWatch.start();
                    final DecodeQualification decodeQualification = productReaderPlugin.getDecodeQualification(productFile);
                    stopWatch.stop();
                    logger.info(INDENT + INDENT + testProduct.getId() + ": " + stopWatch.getTimeDiffString());

                    final String message = productReaderPlugin.getClass().getName() + ": " + testProduct.getRelativePath();
                    assertEquals(message, expected, decodeQualification);
                } else {
                    logProductNotExistent(testProduct);
                }
            }
        }
    }

    @Test
    public void testReadIntendedProductContent() throws IOException {
        final ArrayList<TestProductReader> testReaders = productReaderList.getTestReaders();
        logger.info("Testing IntendedProductContent:");
        for (TestProductReader testReader : testReaders) {
            final ArrayList<String> intendedProductIds = testReader.getIntendedProductIds();
            logger.info(INDENT + testReader.getProductReaderPlugin().getClass().getSimpleName());
            for (String productId : intendedProductIds) {
                final TestProduct testProduct = testProductList.geById(productId);
                if (testProduct.exists()) {
                    logger.info(INDENT + INDENT + productId);
                    final File testProductFile = getTestProductFile(testProduct);

                    final ProductReader productReader = testReader.getProductReaderPlugin().createReaderInstance();
                    final Product product = productReader.readProductNodes(testProductFile, null);
                    try {
                        final ExpectedContent expectedContent = testReader.getExpectedContent(productId);
                        if (expectedContent != null) {
                            testExpectedContent(expectedContent, product);
                        }
                    } finally {
                        if (product != null) {
                            product.dispose();
                        }
                    }
                } else {
                    logProductNotExistent(testProduct);
                }
            }
        }
    }

    @Test
    public void testProductIO_readProduct() throws Exception {
        logger.info("Testing ProductIO.readProduct");
        final StopWatch stopWatch = new StopWatch();
        for (TestProduct testProduct : testProductList) {
            if (testProduct.exists()) {
                final File testProductFile = getTestProductFile(testProduct);
                try {
                    stopWatch.start();
                    ProductIO.readProduct(testProductFile);
                    stopWatch.stop();
                    logger.info(INDENT + testProduct.getId() + " " + stopWatch.getTimeDiffString());
                } catch (Exception e) {
                    final String message = "ProductIO.readProduct " + testProduct.getId() + " caused an exception.\n" +
                                           "Should only return NULL or a product instance but should not cause any exception.";
                    logger.log(Level.SEVERE, message, e);
                    fail(message);
                }
            }else {
                logProductNotExistent(testProduct);
            }
        }

    }

    private static void testExpectedContent(ExpectedContent expectedContent, Product product) {
        final ExpectedBand[] expectedBands = expectedContent.getBands();
        for (final ExpectedBand expectedBand : expectedBands) {
            final Band band = product.getBand(expectedBand.getName());
            assertNotNull("missing band '" + expectedBand.getName() + " in product '" + product.getFileLocation(), band);

            final ExpectedPixel[] expectedPixel = expectedBand.getExpectedPixel();
            for (ExpectedPixel pixel : expectedPixel) {
                final float bandValue = band.getSampleFloat(pixel.getX(), pixel.getY());
                assertEquals(pixel.getValue(), bandValue, 1e-6);
            }
        }
    }

    private static DecodeQualification getExpectedDecodeQualification(TestProductReader testReader, TestProduct testProduct) {
        final ArrayList<String> intendedProductNames = testReader.getIntendedProductIds();
        if (intendedProductNames.contains(testProduct.getId())) {
            return DecodeQualification.INTENDED;
        }

        final ArrayList<String> suitableProductNames = testReader.getSuitableProductIds();
        if (suitableProductNames.contains(testProduct.getId())) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }

    private static File getTestProductFile(TestProduct testProduct) {
        final String relativePath = testProduct.getRelativePath();
        final File testProductFile = new File(dataRootDir, relativePath);
        assertTrue(testProductFile.exists());
        return testProductFile;
    }

    private static void logProductNotExistent(TestProduct testProduct) {
        logger.info(INDENT + INDENT + testProduct.getId() + " not existent");
    }

    private static void readTestDataDirProperty() {
        final String dataDirProperty = System.getProperty(PROPERTYNAME_DATA_DIR);
        if (dataDirProperty == null) {
            fail("Data directory path not set");
        }
        dataRootDir = new File(dataDirProperty);
        if (!dataRootDir.isDirectory()) {
            fail("Data directory is not valid: " + dataDirProperty);
        }
    }

    private static void readProductsList() throws IOException {
        final URL resource = ProductReaderAcceptanceTest.class.getResource("products.json");
        final File productsFile = new File(resource.getPath());
        assertTrue(productsFile.isFile());

        final ObjectMapper mapper = new ObjectMapper();
        testProductList = mapper.readValue(productsFile, ProductList.class);
    }

    private static void validateProductList() {
        for (TestProduct testProduct : testProductList) {
            final String relativePath = testProduct.getRelativePath();
            final File productFile = new File(dataRootDir, relativePath);
            if (!productFile.exists()) {
                testProduct.exists(false);
                if (FAIL_ON_MISSING_DATA) {
                    fail("test product does not exist: " + productFile.getAbsolutePath());
                }
            }
        }
    }

    private static void readProductReadersList() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Iterable<ProductReaderPlugIn> readerPlugIns = SystemUtils.loadServices(ProductReaderPlugIn.class);
        productReaderList = new ProductReaderList();

        for (ProductReaderPlugIn readerPlugIn : readerPlugIns) {
            final Class<? extends ProductReaderPlugIn> readerPlugInClass = readerPlugIn.getClass();
            final String resourceFilename = getReaderTestResourceName(readerPlugInClass.getName());
            URL testConfigUrl = readerPlugInClass.getResource(resourceFilename);
            if (testConfigUrl == null) {
                fail("Unable to load reader test config file: " + resourceFilename);
            }

            final File readerConfigFile = new File(testConfigUrl.getPath());
            assertTrue(readerConfigFile.isFile());

            final TestProductReader testProductReader = mapper.readValue(readerConfigFile, TestProductReader.class);
            testProductReader.setProductReaderPlugin(readerPlugIn);
            productReaderList.add(testProductReader);
        }
    }

    private static String getReaderTestResourceName(String fullyQualifiedName) {
        final String path = fullyQualifiedName.replace(".", "/");
        return "/" + path + "-test.json";
    }

}
