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

package com.bc.ceres.swing.figure.support;

import junit.framework.TestCase;

import java.awt.Color;

import com.bc.ceres.binding.ConversionException;

public class CssColorConverterTest extends TestCase {
    public void testIt() throws ConversionException {
        CssColorConverter colorConverter = new CssColorConverter();

        assertEquals(Color.class, colorConverter.getValueType());

        assertEquals(Color.decode("0x12f45a"), colorConverter.parse("#12F45a"));
        assertEquals(Color.GREEN, colorConverter.parse("00ff00"));

        assertEquals("#12f45a", colorConverter.format(Color.decode("0x12f45a")));
        assertEquals("#00ff00", colorConverter.format(Color.GREEN));

        try {
            colorConverter.parse("x");
            fail("ConversionException?");
        } catch (ConversionException e) {
            // ok
        }

    }
}
