package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelSource;
import junit.framework.TestCase;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;


public class DefaultMultiLevelSourceTest extends TestCase {

    public void testNull() {
        final MultiLevelSource mls = DefaultMultiLevelSource.NULL;
        assertEquals(1, mls.getModel().getLevelCount());
        assertNull(mls.getModel().getModelBounds());
    }

    public void testLevelImages() {
        final PlanarImage src = createSourceImage(256, 128);

        DefaultMultiLevelSource mls = new DefaultMultiLevelSource(src, 5);
        assertEquals(5, mls.getModel().getLevelCount());

        assertSame(src, mls.getSourceImage());
        assertSame(src, mls.getImage(0));

        testLevelImage(mls, 0, 256, 128);
        testLevelImage(mls, 1, 128, 64);
        testLevelImage(mls, 2, 64, 32);
        testLevelImage(mls, 3, 32, 16);
        testLevelImage(mls, 4, 16, 8);
    }

    private void testLevelImage(DefaultMultiLevelSource mls, int level, int ew, int eh) {
        final RenderedImage image = mls.getImage(level);
        assertSame(image, mls.getImage(level));
        assertEquals(mls.getSourceImage().getSampleModel().getDataType(), image.getSampleModel().getDataType());
        assertEquals(mls.getSourceImage().getSampleModel().getNumBands(), image.getSampleModel().getNumBands());
        assertEquals(ew, image.getWidth());
        assertEquals(eh, image.getHeight());
    }

    static PlanarImage createSourceImage(int w, int h) {
        final BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        bi.getRaster().setSample(0, 0, 0, 0);
        bi.getRaster().setSample(1, 0, 0, 1);
        bi.getRaster().setSample(0, 1, 0, 2);
        bi.getRaster().setSample(1, 1, 0, 3);
        return PlanarImage.wrapRenderedImage(bi);
    }
}