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
public class ShapeSymbolTest {

    @Test
    public void testContainsPoint() throws Exception {
        ShapeSymbol shapeSymbol = ShapeSymbol.createCircle(10.0);
        assertEquals(false, shapeSymbol.isHitBy(-7.0, 0.0));
        assertEquals(false, shapeSymbol.isHitBy(-6.0, 0.0));
        assertEquals(false, shapeSymbol.isHitBy(-5.0, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(-4.0, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(-3.0, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(-2, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(-1.0, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(0.0, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(1.0, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(2.0, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(3.0, 0.0));
        assertEquals(true, shapeSymbol.isHitBy(4.0, 0.0));
        assertEquals(false, shapeSymbol.isHitBy(5.0, 0.0));
        assertEquals(false, shapeSymbol.isHitBy(6.0, 0.0));
        assertEquals(false, shapeSymbol.isHitBy(7.0, 0.0));
    }

    @Test
    public void testGetBounds() throws Exception {
        ShapeSymbol shapeSymbol = ShapeSymbol.createCircle(10.0);
        assertEquals(new Rectangle2D.Double(-5, -5, 10, 10), shapeSymbol.getBounds());
    }
}
