package org.esa.beam.framework.datamodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class MathMultiLevelImageTest {

    private Product product;
    private VirtualBand virtualBand;
    private MathMultiLevelImage image;

    @Before
    public void setup() {
        product = new Product("P", "T", 1, 1);
        virtualBand = new VirtualBand("V", ProductData.TYPE_INT8, 1, 1, "1");
        product.addBand(virtualBand);
        image = new MathMultiLevelImage("V != 1", product);
    }

    @Test
    public void imageIsUpdated() {
        assertTrue(0 == image.getImage(0).getData().getSample(0, 0, 0));
        virtualBand.setExpression("0");
        assertTrue(0 != image.getImage(0).getData().getSample(0, 0, 0));
    }

    @Test
    public void listenerIsAdded() {
        assertTrue(Arrays.asList(product.getProductNodeListeners()).contains(image));
    }

    @Test
    public void listenerIsRemoved() {
        image.dispose();
        assertFalse(Arrays.asList(product.getProductNodeListeners()).contains(image));
    }

    @Test
    public void nodeIsAdded() {
        assertTrue(image.getNodeList().contains(virtualBand));
    }

    @Test
    public void nodeIsRemoved() {
        assertTrue(product.removeBand(virtualBand));
        assertFalse(image.getNodeList().contains(virtualBand));
    }

    @Test
    public void nodeListIsCleared() {
        image.dispose();
        assertTrue(image.getNodeList().isEmpty());
    }
}
