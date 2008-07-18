package com.bc.ceres.glayer;

import junit.framework.TestCase;

import java.awt.*;


public class BufferedImageRenderingTest extends TestCase {
    public void testViewIsAnImage() {
        final Rendering rendering = new BufferedImageRendering(150, 150);

        ShapeLayer sl = new ShapeLayer(new Shape[]{
                new Rectangle(0, 0, 100, 100),
                new Rectangle(50, 50, 100, 100),
        });

        sl.render(rendering);
    }

}
