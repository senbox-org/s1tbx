package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.FigureStyle;
import junit.framework.TestCase;

import java.awt.Color;

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
        FigureStyle style = DefaultFigureStyle.createPolygonStyle(Color.RED, null, 0.0);
        assertEquals(Color.RED, style.getFillPaint());
        assertEquals(null, style.getStrokePaint());
        assertEquals(0.0, style.getStrokeWidth());
        assertNull(style.getStroke());

        style = DefaultFigureStyle.createPolygonStyle(Color.RED, Color.BLUE);
        assertEquals(Color.RED, style.getFillPaint());
        assertEquals(Color.BLUE, style.getStrokePaint());
        assertNotNull(style.getStroke());
    }

    public void testFormatAsCss() {
        FigureStyle style;

        style = DefaultFigureStyle.createLineStyle(Color.BLUE, 5.0);
        assertEquals("stroke:#0000ff; stroke-width:5.0", style.toCssString());
        style = DefaultFigureStyle.createPolygonStyle(Color.RED, Color.BLUE, 5.0);
        assertEquals("fill:#ff0000; stroke:#0000ff; stroke-width:5.0", style.toCssString());
        style = DefaultFigureStyle.createPolygonStyle(Color.decode("0x3f4a0d"), Color.decode("0xaabbff"), 0.0);
        assertEquals("fill:#3f4a0d; stroke:#aabbff; stroke-width:0.0", style.toCssString());
        style = DefaultFigureStyle.createPolygonStyle(new Color(12,23,34,127));
        assertEquals("fill:#0c1722; fill-opacity:0.5", style.toCssString());
        style = DefaultFigureStyle.createPolygonStyle(new Color(12,23,34,127), new Color(100, 100, 100, 98));
        assertEquals("fill:#0c1722; fill-opacity:0.5; stroke:#646464; stroke-opacity:0.38; stroke-width:0.0", style.toCssString());
    }
}
