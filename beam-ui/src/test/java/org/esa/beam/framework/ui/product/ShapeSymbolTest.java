package org.esa.beam.framework.ui.product;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        assertEquals(false, shapeSymbol.containsPoint(-7.0, 0.0));
        assertEquals(false, shapeSymbol.containsPoint(-6.0, 0.0));
        assertEquals(false, shapeSymbol.containsPoint(-5.0, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(-4.0, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(-3.0, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(-2, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(-1.0, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(0.0, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(1.0, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(2.0, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(3.0, 0.0));
        assertEquals(true, shapeSymbol.containsPoint(4.0, 0.0));
        assertEquals(false, shapeSymbol.containsPoint(5.0, 0.0));
        assertEquals(false, shapeSymbol.containsPoint(6.0, 0.0));
        assertEquals(false, shapeSymbol.containsPoint(7.0, 0.0));
    }
}
