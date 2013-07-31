package org.esa.beam.dataio;


import org.codehaus.jackson.map.ObjectMapper;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.SystemUtils;
import org.junit.Before;
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

    private ProductList testProductList;
    private ProductReaderList productReaderList;

    @Before
    public void setUp() throws IOException {
        readTestProductsList();
        readProductReadersList();
    }

    @Test
    public void testPluginDecodeQualifications() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final ArrayList<TestProductReader> testReaders = productReaderList.getTestReaders();
        for (TestProductReader testReader : testReaders) {
            final ProductReaderPlugIn productReaderPlugin = createProductReaderPlugIn(testReader);

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
        final ArrayList<String> intendedProductNames = testReader.getIntendedProductNames();
        if (intendedProductNames.contains(testProduct.getId())) {
            return DecodeQualification.INTENDED;
        }

        final ArrayList<String> suitableProductNames = testReader.getSuitableProductNames();
        if (suitableProductNames.contains(testProduct.getId())) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }

    private File getTestProductFile(TestProduct testProduct) {
        final String relativePath = testProduct.getRelativePath();
        final String name = testProduct.getId();
        final String filePath = "C:/Data" + File.separator + relativePath + File.separator + name;
        final File testProductFile = new File(filePath);
        assertTrue(testProductFile.exists());
        return testProductFile;
    }

    private ProductReaderPlugIn createProductReaderPlugIn(TestProductReader testReader) {
        try {
            final String pluginClassName = testReader.getPluginClassName();
            final Class<?> pluginClass = Class.forName(pluginClassName);
            return (ProductReaderPlugIn) pluginClass.newInstance();
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }

        final String message = "Failed to instantiate ProductReaderPlugin: " + testReader.getPluginClassName();
        System.err.println(message);
        fail(message);
        return null;
    }

    private void readTestProductsList() throws IOException {
        final URL resource = ProductReaderAcceptanceTest.class.getResource("products.json");
        final File productsFile = new File(resource.getPath());
        assertTrue(productsFile.isFile());

        final ObjectMapper mapper = new ObjectMapper();
        testProductList = mapper.readValue(productsFile, ProductList.class);
    }

    private void readProductReadersList() throws IOException {
        final Iterable<ProductReaderPlugIn> readerPlugIns = SystemUtils.loadServices(ProductReaderPlugIn.class);

        for (ProductReaderPlugIn readerPlugIn : readerPlugIns) {
            final Class<? extends ProductReaderPlugIn> readerPlugInClass = readerPlugIn.getClass();
            final String resourceFilename = getReaderTestResourceName(readerPlugInClass.getName());

            final URL testConfigUrl = readerPlugInClass.getResource(resourceFilename);
            if (testConfigUrl == null) {
                fail("Unable to load reader test config file: " + resourceFilename);
            }
            final File readerConfigFile = new File(testConfigUrl.getPath());
            assertTrue(readerConfigFile.isFile());
        }


//        final URL resource = ProductReaderAcceptanceTest.class.getResource("readers.json");
//        final File readerssFile = new File(resource.getPath());
//        assertTrue(readerssFile.isFile());
//
//        final ObjectMapper mapper = new ObjectMapper();
//        productReaderList = mapper.readValue(readerssFile, ProductReaderList.class);

        productReaderList = new ProductReaderList();
    }

    private String getReaderTestResourceName(String fullyQualifiedName) {
        final String name = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf(".") + 1);
        return name + "-test.json";
    }
}
