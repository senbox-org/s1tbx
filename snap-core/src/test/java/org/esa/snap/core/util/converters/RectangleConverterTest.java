/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.util.converters;

import com.bc.ceres.binding.ConversionException;
import junit.framework.TestCase;

import java.awt.Rectangle;

/**
 * A layer for vector data nodes.
 *
 * @author Luis Veci
 */
public class RectangleConverterTest extends TestCase {

    public void testParseSuccess() throws ConversionException {
        final RectangleConverter rectConverter = new RectangleConverter();

        final Rectangle rect = rectConverter.parse("1,2,3,4");
        assertNotNull(rect);
        assertEquals(1, rect.x);
        assertEquals(2, rect.y);
        assertEquals(3, rect.width);
        assertEquals(4, rect.height);
    }

    public void testFormatSuccess() throws ConversionException {
        final RectangleConverter rectConverter = new RectangleConverter();

        assertEquals("1,2,3,4", rectConverter.format(new Rectangle(1,2,3,4)));
    }

    public void testFailure() {
        final RectangleConverter rectConverter = new RectangleConverter();

        try {
            rectConverter.parse("");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }

        try {
            rectConverter.parse("string");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }

        try {
            rectConverter.parse("1,2");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }
    }
}
