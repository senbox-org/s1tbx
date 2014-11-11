package com.bc.ceres.swing.figure.support;

import org.junit.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: nfomferra
 * Date: 5/27/11
 * Time: 5:40 PM
 */
public class ImageSymbolTest {

    @Test
    public void testFactoryWithoutRef() throws Exception {
        ImageSymbol imageSymbol = ImageSymbol.createIcon("TestSymbolIcon.png");
        assertNotNull(imageSymbol);
        assertNotNull(imageSymbol.getImage());
        assertEquals(16, imageSymbol.getImage().getWidth());
        assertEquals(8, imageSymbol.getImage().getHeight());
        assertEquals(8.0, imageSymbol.getRefX(), 1E-10);
        assertEquals(4.0, imageSymbol.getRefY(), 1E-10);
        assertEquals(new Rectangle2D.Double(-8, -4, 16, 8), imageSymbol.getBounds());
    }

    @Test
    public void testFactoryWithRef() throws Exception {
        ImageSymbol imageSymbol = ImageSymbol.createIcon("/com/bc/ceres/swing/figure/support/TestSymbolIcon.png", 5.0, 3.0);
        assertNotNull(imageSymbol);
        assertNotNull(imageSymbol.getImage());
        assertEquals(16, imageSymbol.getImage().getWidth());
        assertEquals(8, imageSymbol.getImage().getHeight());
        assertEquals(5.0, imageSymbol.getRefX(), 1E-10);
        assertEquals(3.0, imageSymbol.getRefY(), 1E-10);
        assertEquals(new Rectangle2D.Double(-5, -3, 16, 8), imageSymbol.getBounds());
    }

    @Test
    public void testContainsPoint() throws Exception {
        // Note that the image we load comprises 4 regions: Red, Green, Blue and Transparent.
        ImageSymbol imageSymbol = ImageSymbol.createIcon("/com/bc/ceres/swing/figure/support/TestSymbolIcon.png");
        assertNotNull(imageSymbol);
        assertEquals(false, imageSymbol.isHitBy(-9.0, 0.0)); // Out of bounds
        assertEquals(true, imageSymbol.isHitBy(-8.0, 0.0)); // Red
        assertEquals(true, imageSymbol.isHitBy(-7.0, 0.0)); // Red
        assertEquals(true, imageSymbol.isHitBy(-6.0, 0.0)); // Red
        assertEquals(true, imageSymbol.isHitBy(-5.0, 0.0)); // Red
        assertEquals(true, imageSymbol.isHitBy(-4.0, 0.0)); // Green
        assertEquals(true, imageSymbol.isHitBy(-3.0, 0.0)); // Green
        assertEquals(true, imageSymbol.isHitBy(-2.0, 0.0)); // Green
        assertEquals(true, imageSymbol.isHitBy(-1.0, 0.0)); // Green
        assertEquals(true, imageSymbol.isHitBy(0.0, 0.0));  // Blue
        assertEquals(true, imageSymbol.isHitBy(1.0, 0.0));  // Blue
        assertEquals(true, imageSymbol.isHitBy(2.0, 0.0));  // Blue
        assertEquals(true, imageSymbol.isHitBy(3.0, 0.0));  // Blue
        assertEquals(false, imageSymbol.isHitBy(4.0, 0.0)); // Transparent
        assertEquals(false, imageSymbol.isHitBy(5.0, 0.0)); // Transparent
        assertEquals(false, imageSymbol.isHitBy(6.0, 0.0)); // Transparent
        assertEquals(false, imageSymbol.isHitBy(7.0, 0.0)); // Transparent
        assertEquals(false, imageSymbol.isHitBy(8.0, 0.0)); // Transparent
        assertEquals(false, imageSymbol.isHitBy(9.0, 0.0)); // Out of bounds
    }
}
