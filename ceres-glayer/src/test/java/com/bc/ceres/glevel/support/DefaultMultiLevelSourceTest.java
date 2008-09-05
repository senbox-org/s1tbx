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
        assertTrue(mls.getModel().getModelBounds().isEmpty());
    }

    public void testLevelImages() {
        final PlanarImage src = createSourceImage(256, 128);

        DefaultMultiLevelSource mrs = new DefaultMultiLevelSource(src, 5);
        assertEquals(5, mrs.getModel().getLevelCount());

        assertSame(src, mrs.getSourceImage());
        assertSame(src, mrs.getLevelImage(0));

        testLevelImage(mrs, 0, 256, 128);
        testLevelImage(mrs, 1, 128, 64);
        testLevelImage(mrs, 2, 64, 32);
        testLevelImage(mrs, 3, 32, 16);
        testLevelImage(mrs, 4, 16, 8);
    }

    private RenderedImage testLevelImage(DefaultMultiLevelSource mrs, int level, int ew, int eh) {
        final RenderedImage l0 = mrs.getLevelImage(level);
        assertSame(l0, mrs.getLevelImage(level));
        assertEquals(mrs.getSourceImage().getSampleModel().getDataType(), l0.getSampleModel().getDataType());
        assertEquals(mrs.getSourceImage().getSampleModel().getNumBands(), l0.getSampleModel().getNumBands());
        assertEquals(ew, l0.getWidth());
        assertEquals(eh, l0.getHeight());
        return l0;
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