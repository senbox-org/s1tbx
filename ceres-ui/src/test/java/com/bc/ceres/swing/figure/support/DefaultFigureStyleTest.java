package com.bc.ceres.swing.figure.support;

import junit.framework.TestCase;

import java.awt.BasicStroke;
import java.awt.Color;

public class DefaultFigureStyleTest extends TestCase {
    public void testConstructors() {
        DefaultFigureStyle style = new DefaultFigureStyle();
        assertNotNull(style.getDefaultStyle());
        assertNull(style.getDefaultStyle().getDefaultStyle());
        assertEquals("", style.getName());
        assertEquals(Color.BLACK, style.getFillPaint());
        assertEquals(null, style.getStrokePaint());
        assertEquals(null, style.getStroke());

        style = new DefaultFigureStyle("X", Color.RED, null, null);
        assertEquals("X", style.getName());
        assertEquals(Color.RED, style.getFillPaint());
        assertEquals(null, style.getStrokePaint());
        assertEquals(null, style.getStroke());

        style = new DefaultFigureStyle("X", Color.RED, Color.BLUE, null);
        assertEquals("X", style.getName());
        assertEquals(Color.RED, style.getFillPaint());
        assertEquals(Color.BLUE, style.getStrokePaint());
        assertNotNull(style.getStroke());

        BasicStroke basicStroke = new BasicStroke(8f);
        style = new DefaultFigureStyle("X", Color.RED, Color.BLUE, basicStroke);
        assertEquals("X", style.getName());
        assertEquals(Color.RED, style.getFillPaint());
        assertEquals(Color.BLUE, style.getStrokePaint());
        assertEquals(basicStroke, style.getStroke());
    }
}
