package org.esa.beam.framework.ui.product;

import org.junit.Before;
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
        assertEquals(false, crossSymbol.containsPoint(-7.0, 0.0));
        assertEquals(false, crossSymbol.containsPoint(-6.0, 0.0));
        assertEquals(false, crossSymbol.containsPoint(-5.0, 0.0));
        assertEquals(true, crossSymbol.containsPoint(-4.0, 0.0));
        assertEquals(true, crossSymbol.containsPoint(-3.0, 0.0));
        assertEquals(true, crossSymbol.containsPoint(-2, 0.0));
        assertEquals(true, crossSymbol.containsPoint(-1.0, 0.0));
        assertEquals(true, crossSymbol.containsPoint(0.0, 0.0));
        assertEquals(true, crossSymbol.containsPoint(1.0, 0.0));
        assertEquals(true, crossSymbol.containsPoint(2.0, 0.0));
        assertEquals(true, crossSymbol.containsPoint(3.0, 0.0));
        assertEquals(true, crossSymbol.containsPoint(4.0, 0.0));
        assertEquals(false, crossSymbol.containsPoint(5.0, 0.0));
        assertEquals(false, crossSymbol.containsPoint(6.0, 0.0));
        assertEquals(false, crossSymbol.containsPoint(7.0, 0.0));
    }
}
