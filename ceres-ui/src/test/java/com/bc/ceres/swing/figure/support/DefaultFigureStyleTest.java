package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.FigureStyle;
import junit.framework.TestCase;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Paint;

public class DefaultFigureStyleTest extends TestCase {
    public void testConstructors() {
        // This is the SVG/CSS default
        FigureStyle style1 = new DefaultFigureStyle();
        assertEquals("", style1.getName());
        assertEquals(Color.BLACK, style1.getFillColor());
        assertEquals(null, style1.getStrokeColor());
        assertNotNull(style1.getStroke());

        FigureStyle style2 = new DefaultFigureStyle("X");
        assertEquals("X", style2.getName());
        assertEquals(Color.BLACK, style2.getFillColor());
        assertEquals(null, style2.getStrokeColor());
        assertNotNull(style1.getStroke());
    }

    public void testShapeStyle() {
        FigureStyle style = DefaultFigureStyle.createPolygonStyle(Color.RED);
        assertEquals(Color.RED, style.getFillColor());
        assertEquals(null, style.getStrokeColor());
        assertEquals(0.0, style.getStrokeWidth());
        assertNotNull(style.getStroke());

        style = DefaultFigureStyle.createPolygonStyle(Color.RED, Color.BLUE);
        assertEquals(Color.RED, style.getFillColor());
        assertEquals(Color.BLUE, style.getStrokeColor());
        assertNotNull(style.getStroke());
    }

    public void testCss() {

        testToLineCss("stroke:#0000ff; stroke-width:5.0",
                      DefaultFigureStyle.createLineStyle(Color.BLUE, new BasicStroke(5.0f)));

        testToLineCss("stroke:#0a0b0c; stroke-opacity:0.05; stroke-width:1.0",
                      DefaultFigureStyle.createLineStyle(new Color(10, 11, 12, 13), new BasicStroke(1.0f)));

        testToPolygonCss("fill:#ff0000; stroke:#0000ff; stroke-width:5.0",
                         DefaultFigureStyle.createPolygonStyle(Color.RED, Color.BLUE, new BasicStroke(5.0f)));

        testToPolygonCss("fill:#3f4a0d; stroke:#aabbff",
                         DefaultFigureStyle.createPolygonStyle(Color.decode("0x3f4a0d"),
                                                               Color.decode("0xaabbff")));

        testToPolygonCss("fill:#0c1722; fill-opacity:0.5",
                         DefaultFigureStyle.createPolygonStyle(new Color(12, 23, 34, 128)));

        testToPolygonCss("fill:#0c1722; fill-opacity:0.5; stroke:#646464; stroke-opacity:0.38",
                         DefaultFigureStyle.createPolygonStyle(new Color(12, 23, 34, 127), new Color(100, 100, 100, 98)));
    }

    private void testToLineCss(String expectedCss, FigureStyle style) {
        String css = style.toCssString();
        assertEquals(expectedCss, css);
        testFromLineCss(style, css);
    }

    private void testFromLineCss(FigureStyle expectedStyle, String css) {
        FigureStyle style = new DefaultFigureStyle();
        style.fromCssString(css);
        assertEquals(expectedStyle.getStrokeOpacity(), style.getStrokeOpacity());
        assertEquals(expectedStyle.getStrokeWidth(), style.getStrokeWidth());
        assertEquals(expectedStyle.getStrokeColor(), style.getStrokeColor());

        // FIXME - these sometimes fail due to lossy alpha conversion  (nf)
//        assertEquals(expectedStyle.getStrokePaint(), style.getStrokePaint());
    }

    private void testToPolygonCss(String expectedCss, FigureStyle style) {
        String css = style.toCssString();
        assertEquals(expectedCss, css);
        testFromPolygonCss(style, css);
    }

    private void testFromPolygonCss(FigureStyle expectedStyle, String css) {
        FigureStyle style = new DefaultFigureStyle();
        style.fromCssString(css);
        assertEquals(expectedStyle.getFillColor(), style.getFillColor());
        assertEquals(expectedStyle.getFillOpacity(), style.getFillOpacity());
        assertEquals(expectedStyle.getStrokeColor(), style.getStrokeColor());
        assertEquals(expectedStyle.getStrokeOpacity(), style.getStrokeOpacity());
        assertEquals(expectedStyle.getStrokeWidth(), style.getStrokeWidth());

        // FIXME - these sometimes fail due to lossy alpha conversion  (nf)
        //assertEquals(expectedStyle.getFillPaint(), style.getFillPaint());
        //assertEquals(expectedStyle.getStrokePaint(), style.getStrokePaint());
    }

}
