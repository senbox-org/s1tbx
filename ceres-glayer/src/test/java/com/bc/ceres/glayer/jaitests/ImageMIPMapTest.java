package com.bc.ceres.glayer.jaitests;

import com.sun.media.jai.codec.FileCacheSeekableStream;
import junit.framework.TestCase;

import javax.media.jai.*;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.StreamDescriptor;
import javax.media.jai.util.ImagingListener;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageMIPMapTest extends TestCase {
    public ImageMIPMapTest() {
        JAI.getDefaultInstance().setImagingListener(new ImagingListener() {
            public boolean errorOccurred(String message, Throwable thrown, Object where, boolean isRetryable) throws RuntimeException {
                System.out.println("JAI error occured: " + message);
                return false;
            }
        });
        final long memoryCapacity = 256L << 10 << 10;
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(memoryCapacity);
        JAI.getDefaultInstance().getTileCache().setMemoryThreshold(0.75f);
    }

    public void testImageMIPMap() throws IOException {

        PlanarImage image = loadImage();
        assertEquals(4, image.getSampleModel().getNumBands());
        testRes(image, 1000, 1104, 256, 256);

        image = selectBand(image);
        assertEquals(1, image.getSampleModel().getNumBands());
        testRes(image, 1000, 1104, 256, 256);

        final ImageMIPMap mipMap = new ImageMIPMap(image,
                                                   AffineTransform.getScaleInstance(0.5, 0.5),
                                                   Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        testRes(mipMap.getImage(0), 1000, 1104, 256, 256);
        testRes(mipMap.getImage(1), 500, 552, 256, 256);
        testRes(mipMap.getImage(2), 250, 276, 250, 256);
        testRes(mipMap.getImage(3), 125, 138, 125, 138);
        testRes(mipMap.getImage(4), 62, 69, 62, 69);
        testRes(mipMap.getImage(5), 31, 34, 31, 34);
        testRes(mipMap.getImage(6), 15, 17, 15, 17);
        testRes(mipMap.getImage(7), 7, 8, 7, 8);
        testRes(mipMap.getImage(8), 3, 4, 3, 4);
        testRes(mipMap.getImage(9), 1, 2, 1, 2);
        try {
            testRes(mipMap.getImage(10), 1, 1, 1, 1);
            fail();
        } catch (Exception e) {
            // expected failure
        }
    }

    private static void testRes(RenderedImage image, int w, int h, int tw, int th) {
        assertEquals(w, image.getWidth());
        assertEquals(h, image.getHeight());
        assertEquals(tw, image.getTileWidth());
        assertEquals(th, image.getTileHeight());
    }

    private static PlanarImage loadImage() throws IOException {
        final InputStream stream = ImageMIPMapTest.class.getResourceAsStream("/images/mapimage.png");
        final RenderedOp image = StreamDescriptor.create(new FileCacheSeekableStream(stream), null, null);
        return new TiledImage(image, 256, 256);
    }

    private static PlanarImage selectBand(PlanarImage source) throws IOException {
        return BandSelectDescriptor.create(source, new int[]{0}, null);
    }
}
