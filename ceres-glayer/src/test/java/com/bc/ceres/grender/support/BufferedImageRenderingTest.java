package com.bc.ceres.grender.support;

import junit.framework.TestCase;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;

import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.glayer.support.ShapeLayer;
import com.bc.ceres.glayer.support.ImageLayer;


public class BufferedImageRenderingTest extends TestCase {
    public void testViewIsAnImage() {
        BufferedImage bi = new BufferedImage(2,2, BufferedImage.TYPE_INT_BGR);
        bi.setRGB(0,0, Color.ORANGE.getRGB());
        bi.setRGB(0,1, Color.BLUE.getRGB());
        bi.setRGB(1,0, Color.GREEN.getRGB());
        bi.setRGB(1,1, Color.YELLOW.getRGB());

        final BufferedImageRendering rendering = new BufferedImageRendering(2, 2);
        ImageLayer il = new ImageLayer(bi);
        il.getStyle().setOpacity(1.0);
        il.render(rendering);
        assertEquals(Color.ORANGE.getRGB(), rendering.getImage().getRGB(0, 0));
        assertEquals(Color.BLUE.getRGB(), rendering.getImage().getRGB(0, 1));
        assertEquals(Color.GREEN.getRGB(), rendering.getImage().getRGB(1, 0));
        assertEquals(Color.YELLOW.getRGB(), rendering.getImage().getRGB(1, 1));
    }
}
