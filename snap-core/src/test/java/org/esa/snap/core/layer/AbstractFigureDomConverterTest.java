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

package org.esa.snap.core.layer;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.snap.core.draw.Figure;
import org.esa.snap.core.draw.LineFigure;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.StringReader;
import java.util.HashMap;

import static org.junit.Assert.*;

public class AbstractFigureDomConverterTest {
    private static HashMap<String, Object> attributes;

    @BeforeClass
    public static void setupClass() {
        attributes = new HashMap<String, Object>();
        attributes.put(Figure.OUTLINED_KEY, true);
        attributes.put(Figure.OUTL_COLOR_KEY, new Color(128, 23, 245));
        attributes.put(Figure.OUTL_COMPOSITE_KEY, AlphaComposite.SrcOver.derive(0.45f));
        attributes.put(Figure.OUTL_STROKE_KEY, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
        attributes.put(Figure.FILLED_KEY, false);
        attributes.put(Figure.FILL_COMPOSITE_KEY, AlphaComposite.SrcOver.derive(0.2f));
        attributes.put(Figure.FILL_PAINT_KEY, Color.BLACK);
        attributes.put(Figure.FILL_STROKE_KEY, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    @Test
    public void testAbstractFigurePersistenncy() throws ConversionException, ValidationException {
        String expectedXml =
                "<figure class=\"org.esa.snap.core.draw.LineFigure\">\n" +
                        "    <shape class=\"java.awt.Rectangle\">\n" +
                        "        <x>12</x>\n" +
                        "        <y>13</y>\n" +
                        "        <width>20</width>\n" +
                        "        <height>40</height>\n" +
                        "    </shape>\n" +
                        "    <attributes>\n" +
                        "        <filled>false</filled>\n" +
                        "        <fillTransparency>0.2</fillTransparency>\n" +
                        "        <fillColor>0,0,0</fillColor>\n" +
                        "        <fillStroke>\n" +
                        "            <width>2.0</width>\n" +
                        "            <cap>1</cap>\n" +
                        "            <join>1</join>\n" +
                        "            <miterlimit>10.0</miterlimit>\n" +
                        "            <dashPhase>0.0</dashPhase>\n" +
                        "        </fillStroke>\n" +
                        "        <outlined>true</outlined>\n" +
                        "        <outlineColor>128,23,245</outlineColor>\n" +
                        "        <outlineTransparency>0.45</outlineTransparency>\n" +
                        "        <outlineStroke>\n" +
                        "            <width>2.0</width>\n" +
                        "            <cap>1</cap>\n" +
                        "            <join>2</join>\n" +
                        "            <miterlimit>10.0</miterlimit>\n" +
                        "            <dashPhase>0.0</dashPhase>\n" +
                        "        </outlineStroke>\n" +
                        "    </attributes>\n" +
                        "</figure>";

        AbstractFigureDomConverter domConverter = new AbstractFigureDomConverter();


        XppDomElement element = new XppDomElement(createDom(expectedXml));
        LineFigure actualFigure = (LineFigure) domConverter.convertDomToValue(element, null);


        LineFigure expectedfigure = new LineFigure(new Rectangle(12, 13, 20, 40), attributes);
        assertEquals(expectedfigure.getShape(), actualFigure.getShape());
        assertEquals(expectedfigure.getAttributes(), actualFigure.getAttributes());

        DefaultDomElement domElement = new DefaultDomElement("figure");
        domConverter.convertValueToDom(expectedfigure, domElement);
        assertEquals(expectedXml, domElement.toXml());
    }

    private XppDom createDom(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml)), domWriter);
        return domWriter.getConfiguration();
    }

}
