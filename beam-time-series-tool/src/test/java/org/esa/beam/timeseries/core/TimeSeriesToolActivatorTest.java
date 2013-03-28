package org.esa.beam.timeseries.core;

import static org.junit.Assert.*;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.junit.*;

import java.io.File;

public class TimeSeriesToolActivatorTest {

    private Product testProduct;
    private String absoluteProductPath1;
    private String absoluteProductPath2;
    private String absOutputPath;

    @Before
    public void setUp() throws Exception {
        absOutputPath = "D:/any/non/relative/path";
        absoluteProductPath1 = absOutputPath + "/and/a/product/file/product1.dim";
        absoluteProductPath2 = absOutputPath + "/and/a/product/file/product2.dim";

        testProduct = new Product("test", "test", 20, 20);
        prepareProduct();
    }

    @Test
    public void testSomething() {
        File testOutputDir = new File(absOutputPath);
        // execution
        TimeSeriesToolActivator.convertAbsolutPathsToRelative(testProduct, testOutputDir);

        assertEquals("and/a/product/file/product1.dim", getPathString(AbstractTimeSeries.PRODUCT_LOCATIONS, 0));
        assertEquals("and/a/product/file/product2.dim", getPathString(AbstractTimeSeries.PRODUCT_LOCATIONS, 1));
        assertEquals("and/a/product/file/product1.dim", getPathString(AbstractTimeSeries.SOURCE_PRODUCT_PATHS, 0));
        assertEquals("and/a/product/file/product2.dim", getPathString(AbstractTimeSeries.SOURCE_PRODUCT_PATHS, 1));
    }

    private String getPathString(final String tsMetadataName, int idx) {
        return testProduct.getMetadataRoot()
                    .getElement(AbstractTimeSeries.TIME_SERIES_ROOT_NAME)
                    .getElement(tsMetadataName)
                    .getElement(tsMetadataName + "." + idx)
                    .getAttributeString(AbstractTimeSeries.PL_PATH);
    }

    private void prepareProduct() {
        final MetadataElement tsRoot = new MetadataElement(AbstractTimeSeries.TIME_SERIES_ROOT_NAME);
        testProduct.getMetadataRoot().addElement(tsRoot);

        final MetadataElement productLocations = new MetadataElement(AbstractTimeSeries.PRODUCT_LOCATIONS);
        tsRoot.addElement(productLocations);

        final MetadataElement sourcePaths = new MetadataElement(AbstractTimeSeries.SOURCE_PRODUCT_PATHS);
        tsRoot.addElement(sourcePaths);

        addProductLocationElements(productLocations);
        addSourceProductPathsElemets(sourcePaths);
    }

    private void addProductLocationElements(MetadataElement productLocations) {
        final MetadataElement location0 = new MetadataElement(AbstractTimeSeries.PRODUCT_LOCATIONS + "." + 0);
        final MetadataElement location1 = new MetadataElement(AbstractTimeSeries.PRODUCT_LOCATIONS + "." + 1);
        productLocations.addElement(location0);
        productLocations.addElement(location1);

        location0.addAttribute(new MetadataAttribute(AbstractTimeSeries.PL_PATH, ProductData.createInstance(absoluteProductPath1), true));
        location0.addAttribute(new MetadataAttribute(AbstractTimeSeries.PL_TYPE, ProductData.createInstance("FILE"), true));
        location1.addAttribute(new MetadataAttribute(AbstractTimeSeries.PL_PATH, ProductData.createInstance(absoluteProductPath2), true));
        location1.addAttribute(new MetadataAttribute(AbstractTimeSeries.PL_TYPE, ProductData.createInstance("FILE"), true));
    }

    private void addSourceProductPathsElemets(MetadataElement sourcePaths) {
        final MetadataElement sourcePath1 = new MetadataElement(AbstractTimeSeries.SOURCE_PRODUCT_PATHS + "." + 0);
        final MetadataElement sourcePath2 = new MetadataElement(AbstractTimeSeries.SOURCE_PRODUCT_PATHS + "." + 1);
        sourcePaths.addElement(sourcePath1);
        sourcePaths.addElement(sourcePath2);

        sourcePath1.addAttribute(new MetadataAttribute(AbstractTimeSeries.PL_PATH, ProductData.createInstance(absoluteProductPath1), true));
        sourcePath2.addAttribute(new MetadataAttribute(AbstractTimeSeries.PL_PATH, ProductData.createInstance(absoluteProductPath2), true));
    }
}
