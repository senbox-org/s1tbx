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

import com.bc.ceres.binding.ConversionException;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertEquals;

public class CssColorConverterTest {

    private CssColorConverter colorConverter;

    @Before
    public void setUp() throws Exception {
        colorConverter = new CssColorConverter();
    }

    @Test
    public void testGetValueType() throws Exception {
        assertEquals(Color.class, colorConverter.getValueType());
    }

    @Test
    public void testParseHexString() throws Exception {
        assertEquals(Color.decode("0x12f45a"), colorConverter.parse("#12F45a"));
        assertEquals(Color.GREEN, colorConverter.parse("00ff00"));

        assertEquals("#12f45a", colorConverter.format(Color.decode("0x12f45a")));
        assertEquals("#00ff00", colorConverter.format(Color.GREEN));
    }

    @Test
    public void testParseCommaSeparatedNumbersString() throws ConversionException {
        assertEquals(Color.WHITE, colorConverter.parse("255,255,255"));
        assertEquals(Color.BLACK, colorConverter.parse("0,0,0"));
        assertEquals(Color.decode("0x356463"), colorConverter.parse("53,100,99"));
    }

    @Test(expected = ConversionException.class)
    public void testParseFaultyExpression_1() throws ConversionException {
        colorConverter.parse("x");
    }

    @Test(expected = ConversionException.class)
    public void testParseFaultyExpression_2() throws ConversionException {
        colorConverter.parse("white");
    }

}
