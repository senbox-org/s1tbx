package org.esa.beam.processor.cloud.internal;

import java.awt.Rectangle;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 06.06.2005
 * Time: 10:18:05
 * To change this template use File | Settings | File Templates.
 */
public class LinebasedFrameSizeCalculatorTest extends TestCase {
    private FrameSizeCalculator frameSizeCalculator;

    public void testInit() {
        Rectangle maxFrameSize;

        assertEquals("count", 20, frameSizeCalculator.getFrameCount());
        maxFrameSize = frameSizeCalculator.getMaxFrameSize();
        assertRectangle("maxFrameSize", 10, 1, maxFrameSize);

        frameSizeCalculator = new LinebasedFrameSizeCalculator(1, 1);
        assertEquals("count", 1, frameSizeCalculator.getFrameCount());
        maxFrameSize = frameSizeCalculator.getMaxFrameSize();
        assertRectangle("maxFrameSize", 1, 1, maxFrameSize);
    }

    public void testGetFrameRect() {
        Rectangle frameRect;

        frameSizeCalculator.addMinFrameSize(10, 3);
        assertEquals("count", 7, frameSizeCalculator.getFrameCount());

        frameRect = frameSizeCalculator.getFrameRect(0);
        assertRectangle("frameRect 0", 0, 0, 10, 3, frameRect);

        frameRect = frameSizeCalculator.getFrameRect(1);
        assertRectangle("frameRect 1", 0, 3, 10, 3, frameRect);

        frameRect = frameSizeCalculator.getFrameRect(5);
        assertRectangle("frameRect 5", 0, 15, 10, 3, frameRect);

        frameRect = frameSizeCalculator.getFrameRect(6);
        assertRectangle("frameRect 6", 0, 18, 10, 2, frameRect);
    }

    public void testGetFrameSizeWithSmallerLastFrame() {
        Rectangle frameRect;

        frameRect = frameSizeCalculator.getFrameRect(0);
        assertRectangle("frameRect 0", 0, 0, 10, 1, frameRect);

        frameRect = frameSizeCalculator.getFrameRect(1);
        assertRectangle("frameRect 1", 0, 1, 10, 1, frameRect);

        frameRect = frameSizeCalculator.getFrameRect(2);
        assertRectangle("frameRect 2", 0, 2, 10, 1, frameRect);

        frameRect = frameSizeCalculator.getFrameRect(19);
        assertRectangle("frameRect 19", 0, 19, 10, 1, frameRect);
    }

    public void testAddMinFrameSize() {
        Rectangle maxFrameSize;

        frameSizeCalculator.addMinFrameSize(5, 1);
        assertEquals("count", 20, frameSizeCalculator.getFrameCount());
        maxFrameSize = frameSizeCalculator.getMaxFrameSize();
        assertRectangle("maxFrameSize", 10, 1, maxFrameSize);

        frameSizeCalculator.addMinFrameSize(10, 1);
        assertEquals("count", 20, frameSizeCalculator.getFrameCount());
        maxFrameSize = frameSizeCalculator.getMaxFrameSize();
        assertRectangle("maxFrameSize", 10, 1, maxFrameSize);

        frameSizeCalculator.addMinFrameSize(10, 2);
        assertEquals("count", 10, frameSizeCalculator.getFrameCount());
        maxFrameSize = frameSizeCalculator.getMaxFrameSize();
        assertRectangle("maxFrameSize", 10, 2, maxFrameSize);

        frameSizeCalculator.addMinFrameSize(10, 3);
        assertEquals("count", 4, frameSizeCalculator.getFrameCount());
        maxFrameSize = frameSizeCalculator.getMaxFrameSize();
        assertRectangle("maxFrameSize", 10, 6, maxFrameSize);
    }

    @Override
    public void setUp() {
        frameSizeCalculator = new LinebasedFrameSizeCalculator(10, 20);
    }

    private static void assertRectangle(String text, int x, int y, int width, int height, Rectangle rect) {
        assertEquals(text + "-x", x, rect.x);
        assertEquals(text + "-y", y, rect.y);
        assertRectangle(text, width, height, rect);
    }

    private static void assertRectangle(String text, int width, int height, Rectangle rect) {
        assertEquals(text + "-width", width, rect.width);
        assertEquals(text + "-height", height, rect.height);
    }
}
