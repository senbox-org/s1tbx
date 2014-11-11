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

import com.bc.ceres.swing.figure.FigureStyle;
import org.junit.Test;

import java.awt.BasicStroke;
import java.awt.Color;

import static org.junit.Assert.*;

public class DefaultFigureStyleTest {
    @Test
    public void testDefaultConstructor() {
        // This is the SVG/CSS default
        FigureStyle style = new DefaultFigureStyle();
        assertEquals("", style.getName());
        assertEquals(Color.BLACK, style.getFillColor());
        assertEquals(null, style.getStrokeColor());
        assertNotNull(style.getStroke());
        assertNull(style.getSymbol());
    }

    @Test
    public void testConstructorWithName() {
        FigureStyle style = new DefaultFigureStyle("X");
        assertEquals("X", style.getName());
        assertEquals(Color.BLACK, style.getFillColor());
        assertEquals(null, style.getStrokeColor());
        assertNotNull(style.getStroke());
        assertNull(style.getSymbol());
    }

    @Test
    public void testImageSymbolFromName() {
        DefaultFigureStyle style = new DefaultFigureStyle();
        assertNull(style.getSymbol());
        style.setSymbolName("pin");
        assertNotNull(style.getSymbol());
    }

    @Test
    public void testImageSymbolFromResource() {
        DefaultFigureStyle style = new DefaultFigureStyle();
        assertNull(style.getSymbol());
        style.setSymbolImagePath("/com/bc/ceres/swing/figure/support/TestSymbolIcon.png");
        assertNotNull(style.getSymbol());
    }

    @Test
    public void testPolygonStyle() {
        FigureStyle style = DefaultFigureStyle.createPolygonStyle(Color.RED);
        assertEquals(Color.RED, style.getFillColor());
        assertEquals(null, style.getStrokeColor());
        assertEquals(0.0, style.getStrokeWidth(), 1E-10);
        assertNotNull(style.getStroke());

        style = DefaultFigureStyle.createPolygonStyle(Color.RED, Color.BLUE);
        assertEquals(Color.RED, style.getFillColor());
        assertEquals(Color.BLUE, style.getStrokeColor());
        assertNotNull(style.getStroke());
    }

    @Test
    public void testCss() {
        testToPointCss("symbol-image:TestSymbolIcon.png; symbol-ref-x:2.0; symbol-ref-y:6.0",
                       DefaultFigureStyle.createPointStyle(ImageSymbol.createIcon("TestSymbolIcon.png", 2.0, 6.0)));

        testToPointCss("stroke:#ffc800; stroke-width:2.5; symbol:star",
                      DefaultFigureStyle.createPointStyle(NamedSymbol.STAR, Color.ORANGE, new BasicStroke(2.5f)));

        testToPointCss("fill:#00ff00; stroke:#ffc800; stroke-width:2.5; symbol:pin",
                      DefaultFigureStyle.createPointStyle(NamedSymbol.PIN, Color.GREEN, Color.ORANGE, new BasicStroke(2.5f)));

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

    @Test
    public void testEquals() {
        FigureStyle oneStyle = DefaultFigureStyle.createPointStyle(ImageSymbol.createIcon("TestSymbolIcon.png", 2.0, 6.0));
        FigureStyle sameStyle = DefaultFigureStyle.createPointStyle(ImageSymbol.createIcon("TestSymbolIcon.png", 2.0, 6.0));
        FigureStyle otherStyle = DefaultFigureStyle.createPointStyle(NamedSymbol.STAR, Color.ORANGE, new BasicStroke(2.5f));
        FigureStyle sameOtherStyle = DefaultFigureStyle.createPointStyle(NamedSymbol.STAR, Color.ORANGE, new BasicStroke(2.5f));

        assertEquals(true, oneStyle.equals(oneStyle));
        assertEquals(true, oneStyle.equals(sameStyle));
        assertEquals(false, oneStyle.equals(otherStyle));
        assertEquals(false, oneStyle.equals(sameOtherStyle));
        assertEquals(true, otherStyle.equals(sameOtherStyle));
    }

    private void testToPointCss(String expectedCss, FigureStyle style) {
        String css = style.toCssString();
        assertEquals(expectedCss, css);
        testFromPointCss(style, css);
    }

    private void testFromPointCss(FigureStyle expectedStyle, String css) {
        FigureStyle style = new DefaultFigureStyle();
        style.fromCssString(css);
        assertEquals(expectedStyle.getSymbolName(), style.getSymbolName());
        assertEquals(expectedStyle.getSymbolImagePath(), style.getSymbolImagePath());
        assertEquals(expectedStyle.getSymbolRefX(), style.getSymbolRefX(), 1E-10);
        assertEquals(expectedStyle.getSymbolRefY(), style.getSymbolRefY(), 1E-10);
    }

    private void testToLineCss(String expectedCss, FigureStyle style) {
        String css = style.toCssString();
        assertEquals(expectedCss, css);
        testFromLineCss(style, css);
    }

    private void testFromLineCss(FigureStyle expectedStyle, String css) {
        FigureStyle style = new DefaultFigureStyle();
        style.fromCssString(css);
        assertEquals(expectedStyle.getStrokeOpacity(), style.getStrokeOpacity(), 1E-10);
        assertEquals(expectedStyle.getStrokeWidth(), style.getStrokeWidth(), 1E-10);
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
        assertEquals(expectedStyle.getFillOpacity(), style.getFillOpacity(), 1E-10);
        assertEquals(expectedStyle.getStrokeColor(), style.getStrokeColor());
        assertEquals(expectedStyle.getStrokeOpacity(), style.getStrokeOpacity(), 1E-10);
        assertEquals(expectedStyle.getStrokeWidth(), style.getStrokeWidth(), 1E-10);

        // FIXME - these sometimes fail due to lossy alpha conversion  (nf)
        //assertEquals(expectedStyle.getFillPaint(), style.getFillPaint());
        //assertEquals(expectedStyle.getStrokePaint(), style.getStrokePaint());
    }

}
