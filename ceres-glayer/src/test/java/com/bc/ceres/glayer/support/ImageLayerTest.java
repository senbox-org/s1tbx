package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.ImagingTestCase;
import com.bc.ceres.glayer.support.ImageLayer;

import javax.media.jai.TiledImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class ImageLayerTest extends ImagingTestCase {
    public void testConstructors() {
        ImageLayer layer;
        final TiledImage image = new TiledImage(new BufferedImage(320, 200, BufferedImage.TYPE_BYTE_GRAY), true);

        layer = new ImageLayer(image);
        assertSame(image, layer.getImage());
        assertEquals(new AffineTransform(), layer.getModelToImageTransform());
        assertEquals(new AffineTransform(), layer.getImageToModelTransform());
        assertEquals(1, layer.getLevelCount());
        assertEquals(false, layer.isConcurrent());


        final AffineTransform i2m = AffineTransform.getTranslateInstance(+100, +200);
        layer = new ImageLayer(image, i2m);
        assertSame(image, layer.getImage());
        assertNotSame(i2m, layer.getImageToModelTransform());
        assertNotSame(i2m, layer.getModelToImageTransform());
        assertEquals(AffineTransform.getTranslateInstance(+100, +200), layer.getImageToModelTransform());
        assertEquals(AffineTransform.getTranslateInstance(-100, -200), layer.getModelToImageTransform());
        assertEquals(1, layer.getLevelCount());
        assertEquals(false, layer.isConcurrent());
    }

    public void testBoundingBox() {
        ImageLayer layer;
        final TiledImage image = new TiledImage(new BufferedImage(320, 200, BufferedImage.TYPE_BYTE_GRAY), true);

        layer = new ImageLayer(image);
        assertNotNull(layer.getBounds());
        assertEquals(new Rectangle2D.Double(0.0, 0.0, 320.0, 200.0), layer.getBounds());

        final AffineTransform i2m = new AffineTransform(0.5, 0, 0, 0.5, -25.5, 50.3);
        layer = new ImageLayer(image, i2m);
        assertNotNull(layer.getBounds());
        assertEquals(new Rectangle2D.Double(-25.5, 50.3, 160.0, 100.0), layer.getBounds());
    }
}
