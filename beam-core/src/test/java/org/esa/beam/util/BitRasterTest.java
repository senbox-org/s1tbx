package org.esa.beam.util;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 20.08.2007
 * Time: 15:25:30
 * To change this template use File | Settings | File Templates.
 */
public class BitRasterTest extends TestCase {

    public void testAccessors() {
        final int width = 13;
        final int height = 5;
        final int size = width * height;

        BitRaster bitRaster = new BitRaster(width, height);

        assertEquals(width, bitRaster.getWidth());
        assertEquals(height, bitRaster.getHeight());

        for (int i = 0; i < size; i++) {
            assertEquals("i=" + i, false, bitRaster.isSet(i));
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bitRaster.set(x, y, true);
            }
        }

        for (int i = 0; i < size; i++) {
            assertEquals("i=" + i, true, bitRaster.isSet(i));
        }

        for (int i = 0; i < size; i++) {
            bitRaster.set(i, false);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assertEquals("x=" + x + ",y=" + y, false, bitRaster.isSet(x, y));
            }
        }

        testAccessors(bitRaster, 12, 4);
        testAccessors(bitRaster, 0, 0);
        testAccessors(bitRaster, 2, 1);
        testAccessors(bitRaster, 8, 2);
    }

    private void testAccessors(BitRaster bitRaster, int x, int y) {
        assertEquals(false, bitRaster.isSet(x, y));
        assertEquals(false, bitRaster.isSet(x + y * bitRaster.getWidth()));
        bitRaster.set(x, y, true);
        assertEquals(true, bitRaster.isSet(x, y));
        assertEquals(true, bitRaster.isSet(x + y * bitRaster.getWidth()));
        bitRaster.set(x, y, false);
        assertEquals(false, bitRaster.isSet(x, y));
        assertEquals(false, bitRaster.isSet(x + y * bitRaster.getWidth()));
    }

}
