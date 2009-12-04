package org.esa.beam.framework.datamodel;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.RenderedImage;
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

        final String expression = "V != 1";
        final MultiLevelModel multiLevelModel = ImageManager.createMultiLevelModel(product);
        final MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.createMask(expression,
                                                     product,
                                                     ResolutionLevel.create(getModel(), level));
            }
        };

        image = new MathMultiLevelImage(expression, product, multiLevelSource);
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
