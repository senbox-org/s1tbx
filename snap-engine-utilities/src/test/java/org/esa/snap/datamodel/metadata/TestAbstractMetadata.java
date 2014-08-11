package org.esa.snap.datamodel.metadata;

import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by lveci on 22/07/2014.
 */
public class TestAbstractMetadata {


    @Test
    public void TestAbstractMetadataSAR() throws IOException {
        final Product product = createTestProduct(10, 10);
        createMetadata(product);

        AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);
        String dem = sarMeta.getAttributeString(AbstractMetadataSAR.DEM);
        assertEquals(dem, "dem");

        AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
        String productName = absMeta.getAttributeString(AbstractMetadata.product_name);
        assertEquals(productName, "name");
    }

    private static Product createProduct(final String type, final int w, final int h) {
        final Product product = new Product("name", type, w, h);

        product.setStartTime(AbstractMetadata.parseUTC("10-MAY-2008 20:30:46.890683"));
        product.setEndTime(AbstractMetadata.parseUTC("10-MAY-2008 20:35:46.890683"));
        product.setDescription("description");

        return product;
    }

    private static Product createTestProduct(final int w, final int h) {

        final Product testProduct = createProduct("ASA_APG_1P", w, h);
        TestUtils.createBand(testProduct, "band1", w, h);

        return testProduct;
    }

    private static void createMetadata(final Product product) throws IOException {
        final AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);
        sarMeta.setAttribute(AbstractMetadataSAR.DEM, "dem");

        final AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
        absMeta.setAttribute(AbstractMetadata.product_name, "name");
    }
}
