package com.bc.ceres.swing.figure.support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: nfomferra
 * Date: 5/27/11
 * Time: 5:40 PM
 */
public class CrossSymbolTest {

    @Test
    public void testContainsPoint() throws Exception {
        CrossSymbol crossSymbol = CrossSymbol.createPlus(10.0);
        assertEquals(false, crossSymbol.isHitBy(-7.0, 0.0));
        assertEquals(false, crossSymbol.isHitBy(-6.0, 0.0));
        assertEquals(false, crossSymbol.isHitBy(-5.0, 0.0));
        assertEquals(true, crossSymbol.isHitBy(-4.0, 0.0));
        assertEquals(true, crossSymbol.isHitBy(-3.0, 0.0));
        assertEquals(true, crossSymbol.isHitBy(-2, 0.0));
        assertEquals(true, crossSymbol.isHitBy(-1.0, 0.0));
        assertEquals(true, crossSymbol.isHitBy(0.0, 0.0));
        assertEquals(true, crossSymbol.isHitBy(1.0, 0.0));
        assertEquals(true, crossSymbol.isHitBy(2.0, 0.0));
        assertEquals(true, crossSymbol.isHitBy(3.0, 0.0));
        assertEquals(true, crossSymbol.isHitBy(4.0, 0.0));
        assertEquals(false, crossSymbol.isHitBy(5.0, 0.0));
        assertEquals(false, crossSymbol.isHitBy(6.0, 0.0));
        assertEquals(false, crossSymbol.isHitBy(7.0, 0.0));
    }
}
