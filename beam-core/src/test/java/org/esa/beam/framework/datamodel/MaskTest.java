package org.esa.beam.framework.datamodel;

import com.bc.ceres.binding.ValueContainer;
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
        Mask mask = new Mask("WATER", 256, 128, imageType, Color.BLUE, 0.7f);
        assertEquals("WATER", mask.getName());
        assertEquals(256, mask.getRasterWidth());
        assertEquals(128, mask.getRasterHeight());
        assertSame(imageType, mask.getImageType());
        ValueContainer imageConfig = mask.getImageConfig();
        assertNotNull(imageConfig);
        assertEquals(Color.BLUE, mask.getImageConfig().getValue("color"));
        assertEquals(0.7f, mask.getImageConfig().getValue("transparency"));

        MultiLevelImage image = mask.getSourceImage();
        assertNotNull(image);
        assertSame(image, mask.getGeophysicalImage());
        assertSame(null, mask.getValidMaskImage());
    }

    @Test
    public void testAbstractMaskImageType() {
        Mask.ImageType type = new NullImageType();
        ValueContainer imageConfig = type.createImageConfig();
        assertEquals(Color.RED, imageConfig.getValue("color"));
        assertEquals(0.5f, imageConfig.getValue("transparency"));
    }

    private static class NullImageType extends Mask.ImageType {
        @Override
        public MultiLevelImage createImage(Mask mask) {
            return null;
        }
    }

    private static class BufferedImageType extends Mask.ImageType {
        @Override
        public MultiLevelImage createImage(Mask mask) {
            BufferedImage image = new BufferedImage(mask.getSceneRasterWidth(), mask.getSceneRasterHeight(), BufferedImage.TYPE_BYTE_GRAY);
            return new DefaultMultiLevelImage(new DefaultMultiLevelSource(image, 3));
        }
    }
}
