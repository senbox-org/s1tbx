package com.bc.ceres.glayer;

import junit.framework.TestCase;

import java.awt.*;
import java.awt.geom.Rectangle2D;


public class BufferedImageRenderingTest extends TestCase {
    public void testViewIsAnImage() {
        final BufferedImageRendering rendering = new BufferedImageRendering(3, 3);

        ShapeLayer sl = new ShapeLayer(new Shape[]{
                new Rectangle2D.Double(0, 0, 2, 2),
        });

        sl.render(rendering);

        assertEquals(Color.BLACK.getRGB(), rendering.getImage().getRGB(0, 0));
        assertEquals(Color.WHITE.getRGB(), rendering.getImage().getRGB(1, 1));
        assertEquals(Color.BLACK.getRGB(), rendering.getImage().getRGB(2, 2));
    }

}
