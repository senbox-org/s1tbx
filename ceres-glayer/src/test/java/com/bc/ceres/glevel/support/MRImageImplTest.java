package com.bc.ceres.glevel.support;

import junit.framework.TestCase;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

public class MRImageImplTest extends TestCase {

    public void testIt() {
        final PlanarImage src = createSourceImage(16, 16);

        MultiResolutionImageImpl mri = new MultiResolutionImageImpl(new MyLevelImageFactory(src));

        final RenderedImage l0 = mri.getLevelImage(0);
        assertSame(l0, mri.getLevelImage(0));
        assertSame(l0, mri.getWrappedImage());
        assertEquals(DataBuffer.TYPE_DOUBLE, l0.getSampleModel().getDataType());
        assertEquals(16, l0.getWidth());
        assertEquals(16, l0.getHeight());

        final RenderedImage l1 = mri.getLevelImage(1);
        assertSame(l1, mri.getLevelImage(1));
        assertEquals(DataBuffer.TYPE_DOUBLE, l1.getSampleModel().getDataType());
        assertEquals(8, l1.getWidth());
        assertEquals(8, l1.getHeight());

        final RenderedImage l2 = mri.getLevelImage(2);
        assertSame(l2, mri.getLevelImage(2));
        assertEquals(DataBuffer.TYPE_DOUBLE, l2.getSampleModel().getDataType());
        assertEquals(4, l2.getWidth());
        assertEquals(4, l2.getHeight());
    }

    static PlanarImage createSourceImage(int w, int h) {
        final BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        bi.getRaster().setSample(0, 0, 0, 0);
        bi.getRaster().setSample(1, 0, 0, 1);
        bi.getRaster().setSample(0, 1, 0, 2);
        bi.getRaster().setSample(1, 1, 0, 3);
        return PlanarImage.wrapRenderedImage(bi);
    }

    private static class MyLevelImageFactory extends LRImageFactoryImpl {

        private MyLevelImageFactory(PlanarImage frSource) {
            super(frSource);
        }

        @Override
        protected RenderedImage createLRTargetImage(RenderedImage lrSource) {
            final RenderedOp dst = FormatDescriptor.create(PlanarImage.wrapRenderedImage(lrSource), DataBuffer.TYPE_DOUBLE, null);
            return MultiplyConstDescriptor.create(dst, new double[]{2.5}, null);
        }
    }
}
