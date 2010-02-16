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
