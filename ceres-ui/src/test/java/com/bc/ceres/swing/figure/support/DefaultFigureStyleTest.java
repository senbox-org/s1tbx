package com.bc.ceres.swing.figure.support;

import junit.framework.TestCase;

import java.awt.BasicStroke;
import java.awt.Color;

import com.bc.ceres.swing.figure.FigureStyle;

public class DefaultFigureStyleTest extends TestCase {
    public void testConstructors() {
        FigureStyle style1 = new DefaultFigureStyle();
        assertNotNull(style1.getDefaultStyle());
        assertNull(style1.getDefaultStyle().getDefaultStyle());
        assertEquals("", style1.getName());
        assertEquals(Color.BLACK, style1.getFillPaint());
        assertEquals(null, style1.getStrokePaint());
        assertEquals(null, style1.getStroke());

        FigureStyle style2 = new DefaultFigureStyle("X", style1);
        assertSame(style1, style2.getDefaultStyle());
        assertEquals("X", style2.getName());
        assertEquals(Color.BLACK, style2.getFillPaint());
        assertEquals(null, style2.getStrokePaint());
        assertEquals(null, style2.getStroke());
    }

     public void testShapeStyle() {
        FigureStyle style = DefaultFigureStyle.createShapeStyle(Color.RED, null, null);
        assertEquals(Color.RED, style.getFillPaint());
        assertEquals(null, style.getStrokePaint());
        assertEquals(null, style.getStroke());

        style = DefaultFigureStyle.createShapeStyle(Color.RED, Color.BLUE, null);
        assertEquals(Color.RED, style.getFillPaint());
        assertEquals(Color.BLUE, style.getStrokePaint());
        assertNotNull(style.getStroke());

        BasicStroke basicStroke = new BasicStroke(8f);
        style = DefaultFigureStyle.createShapeStyle(Color.RED, Color.BLUE, basicStroke);
        assertEquals(Color.RED, style.getFillPaint());
        assertEquals(Color.BLUE, style.getStrokePaint());
        assertEquals(basicStroke, style.getStroke());
    }
}
