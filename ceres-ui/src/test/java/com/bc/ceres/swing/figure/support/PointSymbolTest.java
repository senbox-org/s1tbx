package com.bc.ceres.swing.figure.support;

import org.junit.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: nfomferra
 * Date: 5/27/11
 * Time: 5:40 PM
 */
public class PointSymbolTest {

    @Test
    public void testContainsPoint() throws Exception {
        PointSymbol pointSymbol = PointSymbol.createPlus(10.0);
        assertEquals(false, pointSymbol.isHitBy(-7.0, 0.0));
        assertEquals(false, pointSymbol.isHitBy(-6.0, 0.0));
        assertEquals(false, pointSymbol.isHitBy(-5.0, 0.0));
        assertEquals(true, pointSymbol.isHitBy(-4.0, 0.0));
        assertEquals(true, pointSymbol.isHitBy(-3.0, 0.0));
        assertEquals(true, pointSymbol.isHitBy(-2, 0.0));
        assertEquals(true, pointSymbol.isHitBy(-1.0, 0.0));
        assertEquals(true, pointSymbol.isHitBy(0.0, 0.0));
        assertEquals(true, pointSymbol.isHitBy(1.0, 0.0));
        assertEquals(true, pointSymbol.isHitBy(2.0, 0.0));
        assertEquals(true, pointSymbol.isHitBy(3.0, 0.0));
        assertEquals(true, pointSymbol.isHitBy(4.0, 0.0));
        assertEquals(false, pointSymbol.isHitBy(5.0, 0.0));
        assertEquals(false, pointSymbol.isHitBy(6.0, 0.0));
        assertEquals(false, pointSymbol.isHitBy(7.0, 0.0));
    }

    @Test
    public void testGetBounds() throws Exception {
        PointSymbol pointSymbol = PointSymbol.createPlus(10.0);
        assertEquals(new Rectangle2D.Double(-5, -5, 10, 10), pointSymbol.getBounds());
    }
}
