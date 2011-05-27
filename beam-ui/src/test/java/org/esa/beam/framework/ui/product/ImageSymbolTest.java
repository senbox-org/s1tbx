package org.esa.beam.framework.ui.product;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        ImageSymbol imageSymbol = ImageSymbol.createIcon(getClass(), "image-symbol.png");
        assertNotNull(imageSymbol);
        assertNotNull(imageSymbol.getImage());
        assertEquals(16, imageSymbol.getImage().getWidth());
        assertEquals(8, imageSymbol.getImage().getHeight());
        assertEquals(8.0, imageSymbol.getRefX(), 1E-10);
        assertEquals(4.0, imageSymbol.getRefY(), 1E-10);
    }

    @Test
    public void testFactoryWithRef() throws Exception {
        ImageSymbol imageSymbol = ImageSymbol.createIcon(getClass(), "image-symbol.png", 5.0, 3.0);
        assertNotNull(imageSymbol);
        assertNotNull(imageSymbol.getImage());
        assertEquals(16, imageSymbol.getImage().getWidth());
        assertEquals(8, imageSymbol.getImage().getHeight());
        assertEquals(5.0, imageSymbol.getRefX(), 1E-10);
        assertEquals(3.0, imageSymbol.getRefY(), 1E-10);
    }

    @Test
    public void testContainsPoint() throws Exception {
        ImageSymbol imageSymbol = ImageSymbol.createIcon(getClass(), "image-symbol.png");
        assertNotNull(imageSymbol);
        assertEquals(false, imageSymbol.containsPoint(-9.0, 0.0));
        assertEquals(true, imageSymbol.containsPoint(-8.0, 0.0));
        assertEquals(true, imageSymbol.containsPoint(-4.0, 0.0));
        assertEquals(true, imageSymbol.containsPoint(0.0, 0.0));
        assertEquals(true, imageSymbol.containsPoint(1.0, 0.0));
        assertEquals(true, imageSymbol.containsPoint(2.0, 0.0));
        assertEquals(true, imageSymbol.containsPoint(3.0, 0.0));
        assertEquals(false, imageSymbol.containsPoint(4.0, 0.0));
        assertEquals(false, imageSymbol.containsPoint(5.0, 0.0));
        assertEquals(false, imageSymbol.containsPoint(6.0, 0.0));
        assertEquals(false, imageSymbol.containsPoint(7.0, 0.0));
        assertEquals(false, imageSymbol.containsPoint(8.0, 0.0));
        assertEquals(false, imageSymbol.containsPoint(9.0, 0.0));
    }
}
