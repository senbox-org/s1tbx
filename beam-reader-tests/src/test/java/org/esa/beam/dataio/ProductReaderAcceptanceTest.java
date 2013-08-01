package org.esa.beam.dataio;


import org.codehaus.jackson.map.ObjectMapper;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.SystemUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

@RunWith(ReaderTestRunner.class)
public class ProductReaderAcceptanceTest {

    private static ProductList testProductList;
    private static ProductReaderList productReaderList;
    private static File dataRootDir;

    @BeforeClass
    public static void initialize() throws IOException {
        readTestDataDirProperty();

        readProductsList();
        validateProductList();

        readProductReadersList();
    }

    private static void validateProductList() {
        final ArrayList<TestProduct> testProducts = testProductList.getTestProducts();
        for (TestProduct testProduct : testProducts) {
            final String relativePath = testProduct.getRelativePath();
            final File productFile = new File(dataRootDir, relativePath);
            if (!productFile.exists()) {
                fail("test product does not exist: " + productFile.getAbsolutePath());
            }
        }
    }

    @Test
    public void testPluginDecodeQualifications() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final ArrayList<TestProductReader> testReaders = productReaderList.getTestReaders();
        for (TestProductReader testReader : testReaders) {
            final ProductReaderPlugIn productReaderPlugin = testReader.getProductReaderPlugin();

            final ArrayList<TestProduct> testProducts = testProductList.getTestProducts();
            for (TestProduct testProduct : testProducts) {
                final File productFile = getTestProductFile(testProduct);

                final DecodeQualification expected = getExpectedDecodeQualification(testReader, testProduct);
                final DecodeQualification decodeQualification = productReaderPlugin.getDecodeQualification(productFile);
                assertEquals(expected, decodeQualification);
            }
        }
    }

    private DecodeQualification getExpectedDecodeQualification(TestProductReader testReader, TestProduct testProduct) {
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

    private File getTestProductFile(TestProduct testProduct) {
        final String relativePath = testProduct.getRelativePath();
        final File testProductFile = new File(dataRootDir, relativePath);
        assertTrue(testProductFile.exists());
        return testProductFile;
    }

    private static void readTestDataDirProperty() {
        final String dataDirProperty = System.getProperty("reader.tests.data.dir");
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

    private static void readProductReadersList() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Iterable<ProductReaderPlugIn> readerPlugIns = SystemUtils.loadServices(ProductReaderPlugIn.class);
        productReaderList = new ProductReaderList();

        for (ProductReaderPlugIn readerPlugIn : readerPlugIns) {
            final Class<? extends ProductReaderPlugIn> readerPlugInClass = readerPlugIn.getClass();
            final String resourceFilename = getReaderTestResourceName(readerPlugInClass.getName());

            final URL testConfigUrl = readerPlugInClass.getResource(resourceFilename);
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
        final String name = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf(".") + 1);
        return name + "-test.json";
    }
}
