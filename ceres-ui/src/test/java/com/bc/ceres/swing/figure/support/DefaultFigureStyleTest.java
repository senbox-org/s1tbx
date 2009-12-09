package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.FigureStyle;
import junit.framework.TestCase;

import java.awt.Color;

public class DefaultFigureStyleTest extends TestCase {
    public void testConstructors() {
        // This is the SVG/CSS default
        FigureStyle style1 = new DefaultFigureStyle();
        assertEquals("", style1.getName());
        assertEquals(Color.BLACK, style1.getFillColor());
        assertEquals(null, style1.getStrokeColor());
        assertEquals(null, style1.getStroke());

        FigureStyle style2 = new DefaultFigureStyle("X");
        assertEquals("X", style2.getName());
        assertEquals(Color.BLACK, style2.getFillColor());
        assertEquals(null, style2.getStrokeColor());
        assertEquals(null, style2.getStroke());
    }

    public void testShapeStyle() {
        FigureStyle style = DefaultFigureStyle.createPolygonStyle(Color.RED, null, 0.0);
        assertEquals(Color.RED, style.getFillColor());
        assertEquals(null, style.getStrokeColor());
        assertEquals(0.0, style.getStrokeWidth());
        assertNull(style.getStroke());

        style = DefaultFigureStyle.createPolygonStyle(Color.RED, Color.BLUE);
        assertEquals(Color.RED, style.getFillColor());
        assertEquals(Color.BLUE, style.getStrokeColor());
        assertNotNull(style.getStroke());
    }

    public void testCss() {

        testToLineCss("stroke:#0000ff; stroke-width:5.0",
                      DefaultFigureStyle.createLineStyle(Color.BLUE, 5.0));

        testToLineCss("stroke:#0a0b0c; stroke-opacity:0.05; stroke-width:1.0",
                      DefaultFigureStyle.createLineStyle(new Color(10, 11, 12, 13), 1.0));

        testToPolygonCss("fill:#ff0000; stroke:#0000ff; stroke-width:5.0",
                         DefaultFigureStyle.createPolygonStyle(Color.RED, Color.BLUE, 5.0));

        testToPolygonCss("fill:#3f4a0d; stroke:#aabbff; stroke-width:0.0",
                         DefaultFigureStyle.createPolygonStyle(Color.decode("0x3f4a0d"),
                                                               Color.decode("0xaabbff"), 0.0));

        testToPolygonCss("fill:#0c1722; fill-opacity:0.5",
                         DefaultFigureStyle.createPolygonStyle(new Color(12, 23, 34, 127)));

        testToPolygonCss("fill:#0c1722; fill-opacity:0.5; stroke:#646464; stroke-opacity:0.38; stroke-width:0.0",
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
        assertEqualsRgb(expectedStyle.getStrokeColor(), style.getStrokeColor());
        assertEquals(expectedStyle.getStrokeOpacity(), style.getStrokeOpacity());
        assertEquals(expectedStyle.getStrokeWidth(), style.getStrokeWidth());
    }

    private void testToPolygonCss(String expectedCss, FigureStyle style) {
        String css = style.toCssString();
        assertEquals(expectedCss, css);
        testFromPolygonCss(style, css);
    }

    private void testFromPolygonCss(FigureStyle expectedStyle, String css) {
        FigureStyle style = new DefaultFigureStyle();
        style.fromCssString(css);
        assertEqualsRgb(expectedStyle.getFillColor(), style.getFillColor());
        assertEquals(expectedStyle.getFillOpacity(), style.getFillOpacity());
        assertEqualsRgb(expectedStyle.getStrokeColor(), style.getStrokeColor());
        assertEquals(expectedStyle.getStrokeOpacity(), style.getStrokeOpacity());
        assertEquals(expectedStyle.getStrokeWidth(), style.getStrokeWidth());
    }

    public static void assertEqualsRgb(Color expected, Color actual) {
        if (expected == null || actual == null) {
            assertSame(expected, actual);
            return;
        }
        assertEquals(expected.getRed(), actual.getRed());
        assertEquals(expected.getGreen(), actual.getGreen());
        assertEquals(expected.getBlue(), actual.getBlue());
    }
}
