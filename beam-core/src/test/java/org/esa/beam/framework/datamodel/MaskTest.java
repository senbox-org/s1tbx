package org.esa.beam.framework.datamodel;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class MaskTest {
    @Test
    public void testMask() {
        Mask.ImageType imageType = new BufferedImageType();
        Mask mask = new Mask("WATER", 256, 128, imageType);
        assertEquals("WATER", mask.getName());
        assertEquals(256, mask.getRasterWidth());
        assertEquals(128, mask.getRasterHeight());
        assertSame(imageType, mask.getImageType());
        PropertyContainer imageConfig = mask.getImageConfig();
        assertNotNull(imageConfig);
        assertEquals(Color.RED, mask.getImageConfig().getValue("color"));
        assertEquals(0.5, mask.getImageConfig().getValue("transparency"));
        MultiLevelImage image = mask.getSourceImage();
        assertNotNull(image);
        assertSame(image, mask.getGeophysicalImage());
        assertSame(null, mask.getValidMaskImage());
    }

    @Test
    public void testAbstractMaskImageType() {
        Mask.ImageType type = new NullImageType();
        PropertyContainer imageConfig = type.createImageConfig();
        assertEquals(Color.RED, imageConfig.getValue("color"));
        assertEquals(0.5, imageConfig.getValue("transparency"));
    }
    
    @Test
    public void testRenameBand() {
        Product product = new Product("t", "d", 1, 1);
        Band band = product.addBand("b", ProductData.TYPE_INT8);
        Mask mask = new Mask("m", 1, 1, Mask.BandMathType.INSTANCE);
        Mask.BandMathType.setExpression(mask, "b == 2");
        product.getMaskGroup().add(mask);
        
        String expression = Mask.BandMathType.getExpression(mask);
        assertEquals("b == 2", expression);
        band.setName("c");
        expression = Mask.BandMathType.getExpression(mask);
        assertEquals("c == 2", expression);
    }

    private static class NullImageType extends Mask.ImageType {

        private NullImageType() {
            super("Null");
        }

        @Override
        public MultiLevelImage createImage(Mask mask) {
            return null;
        }
    }

    private static class BufferedImageType extends Mask.ImageType {

        private BufferedImageType() {
            super("Buffered");
        }

        @Override
        public MultiLevelImage createImage(Mask mask) {
            BufferedImage image = new BufferedImage(mask.getSceneRasterWidth(), mask.getSceneRasterHeight(), BufferedImage.TYPE_BYTE_GRAY);
            return new DefaultMultiLevelImage(new DefaultMultiLevelSource(image, 3));
        }
    }
}
