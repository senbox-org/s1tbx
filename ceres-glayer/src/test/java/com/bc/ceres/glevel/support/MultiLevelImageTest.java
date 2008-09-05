package com.bc.ceres.glevel.support;

import junit.framework.TestCase;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class MultiLevelImageTest extends TestCase {
    public void testAllProperties() {
        final PlanarImage sourceImage = createSourceImage(2, 2);
        final DefaultMultiLevelSource mls = new DefaultMultiLevelSource(sourceImage);
        final MultiLevelImage mli = new MultiLevelImage(mls);

        assertSame(mls, mli.getMultiLevelSource());
        assertSame(sourceImage, mli.getWrappedImage());
        assertSame(mls.getLevelImage(0), mli.getWrappedImage());

        assertEquals(0, mli.getData().getSample(0, 0, 0));
        assertEquals(1, mli.getData().getSample(1, 0, 0));
        assertEquals(2, mli.getData().getSample(0, 1, 0));
        assertEquals(3, mli.getData().getSample(1, 1, 0));
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
