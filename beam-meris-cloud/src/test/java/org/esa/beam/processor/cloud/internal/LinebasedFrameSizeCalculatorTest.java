/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.processor.cloud.internal;

import java.awt.Rectangle;

import junit.framework.TestCase;


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
