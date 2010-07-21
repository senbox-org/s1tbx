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
package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;
import org.esa.beam.dataio.dimap.DimapDocumentTest;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.XmlWriter;
import org.jdom.Element;

import java.awt.geom.GeneralPath;
import java.io.StringWriter;

public class ROIDefinitionTest extends TestCase {

    private final String _ls = SystemUtils.LS;

    public ROIDefinitionTest(String name) {
        super(name);
    }

    public void testToMask() {
        final ROIDefinition roiDefinition = new ROIDefinition();
        roiDefinition.setBitmaskExpr("true");
        roiDefinition.setBitmaskEnabled(true);
        roiDefinition.setValueRangeMin(1.0f);
        roiDefinition.setValueRangeMax(2.0f);
        roiDefinition.setValueRangeEnabled(true);
        roiDefinition.setInverted(true);

        final Mask mask = ROIDefinition.toMask(roiDefinition, new Band("B", ProductData.TYPE_INT8, 10, 10));
        assertEquals("!(true && (B >= 1.0 && B <= 2.0))", Mask.BandMathsType.getExpression(mask));
    }

    public void testWriteXml() {
        StringWriter stringWriter;
        String expexted = "";

        final ROIDefinition roiDefinition = new ROIDefinition();
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION />" + _ls +
                   "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>false</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>false</INVERTED>" + _ls +
                   "    <OR_COMBINED>false</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>false</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setBitmaskEnabled(true);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION />" + _ls +
                   "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>false</INVERTED>" + _ls +
                   "    <OR_COMBINED>false</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>false</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setInverted(true);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION />" + _ls +
                   "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>false</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>false</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setOrCombined(true);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION />" + _ls +
                   "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>true</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>false</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setShapeEnabled(true);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION />" + _ls +
                   "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>true</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>true</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setValueRangeEnabled(true);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION />" + _ls +
                   "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>true</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>true</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>true</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setValueRangeMax(34.5f);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION />" + _ls +
                   "    <VALUE_RANGE_MAX>34.5</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>true</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>true</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>true</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setValueRangeMin(-21.342f);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION />" + _ls +
                   "    <VALUE_RANGE_MAX>34.5</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>-21.342</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>true</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>true</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>true</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setBitmaskExpr("flags.land");
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION>flags.land</EXPRESSION>" + _ls +
                   "    <VALUE_RANGE_MAX>34.5</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>-21.342</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>true</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>true</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>true</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setPinUseEnabled(true);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION>flags.land</EXPRESSION>" + _ls +
                   "    <VALUE_RANGE_MAX>34.5</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>-21.342</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>true</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>true</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>true</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>true</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        roiDefinition.setPinUseEnabled(false);
        expexted = "" +
                   "<ROI_Definition>" + _ls +
                   "    <EXPRESSION>flags.land</EXPRESSION>" + _ls +
                   "    <VALUE_RANGE_MAX>34.5</VALUE_RANGE_MAX>" + _ls +
                   "    <VALUE_RANGE_MIN>-21.342</VALUE_RANGE_MIN>" + _ls +
                   "    <BITMASK_ENABLED>true</BITMASK_ENABLED>" + _ls +
                   "    <INVERTED>true</INVERTED>" + _ls +
                   "    <OR_COMBINED>true</OR_COMBINED>" + _ls +
                   "    <SHAPE_ENABLED>true</SHAPE_ENABLED>" + _ls +
                   "    <VALUE_RANGE_ENABLED>true</VALUE_RANGE_ENABLED>" + _ls +
                   "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                   "</ROI_Definition>";
        stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());
    }

    public void testWriteXmlWidthLine_AndInitFromJDOM() {
        final ROIDefinition roiDefinition = new ROIDefinition();
        roiDefinition.setShapeFigure(ShapeFigure.createLine(2f, 3f, 4f, 5f, null));

        final String expexted = "" +
                                "<ROI_Definition>" + _ls +
                                "    <EXPRESSION />" + _ls +
                                "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                                "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                                "    <BITMASK_ENABLED>false</BITMASK_ENABLED>" + _ls +
                                "    <INVERTED>false</INVERTED>" + _ls +
                                "    <OR_COMBINED>false</OR_COMBINED>" + _ls +
                                "    <SHAPE_ENABLED>false</SHAPE_ENABLED>" + _ls +
                                "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                                "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                                "    <ROI_ONE_DIMENSIONS>true</ROI_ONE_DIMENSIONS>" + _ls +
                                "    <Shape_Figure type=\"Line2D\" value=\"2.0,3.0,4.0,5.0\" />" + _ls +
                                "</ROI_Definition>";
        StringWriter stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        final ROIDefinition newRoiDefinition = new ROIDefinition();
        newRoiDefinition.initFromJDOMElem(DimapDocumentTest.createJDOMElement(roiDefinition));

        stringWriter = new StringWriter();
        newRoiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());
    }

    public void testWriteXmlWidthEllipse_AndInitFromJDOM() {
        final ROIDefinition roiDefinition = new ROIDefinition();
        roiDefinition.setShapeFigure(ShapeFigure.createEllipseArea(1f, 2f, 3f, 4f, null));

        final String expexted = "" +
                                "<ROI_Definition>" + _ls +
                                "    <EXPRESSION />" + _ls +
                                "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                                "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                                "    <BITMASK_ENABLED>false</BITMASK_ENABLED>" + _ls +
                                "    <INVERTED>false</INVERTED>" + _ls +
                                "    <OR_COMBINED>false</OR_COMBINED>" + _ls +
                                "    <SHAPE_ENABLED>false</SHAPE_ENABLED>" + _ls +
                                "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                                "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                                "    <ROI_ONE_DIMENSIONS>false</ROI_ONE_DIMENSIONS>" + _ls +
                                "    <Shape_Figure type=\"Ellipse2D\" value=\"1.0,2.0,3.0,4.0\" />" + _ls +
                                "</ROI_Definition>";
        StringWriter stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        final ROIDefinition newRoiDefinition = new ROIDefinition();
        newRoiDefinition.initFromJDOMElem(DimapDocumentTest.createJDOMElement(roiDefinition));

        stringWriter = new StringWriter();
        newRoiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());
    }

    public void testWriteXmlWidthRectangle_AndInitFromJDOM() {
        final ROIDefinition roiDefinition = new ROIDefinition();
        roiDefinition.setShapeFigure(ShapeFigure.createRectangleArea(3f, 5f, 20f, 15f, null));

        final String expexted = "" +
                                "<ROI_Definition>" + _ls +
                                "    <EXPRESSION />" + _ls +
                                "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                                "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                                "    <BITMASK_ENABLED>false</BITMASK_ENABLED>" + _ls +
                                "    <INVERTED>false</INVERTED>" + _ls +
                                "    <OR_COMBINED>false</OR_COMBINED>" + _ls +
                                "    <SHAPE_ENABLED>false</SHAPE_ENABLED>" + _ls +
                                "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                                "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                                "    <ROI_ONE_DIMENSIONS>false</ROI_ONE_DIMENSIONS>" + _ls +
                                "    <Shape_Figure type=\"Rectangle2D\" value=\"3.0,5.0,20.0,15.0\" />" + _ls +
                                "</ROI_Definition>";
        StringWriter stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        final ROIDefinition newRoiDefinition = new ROIDefinition();
        newRoiDefinition.initFromJDOMElem(DimapDocumentTest.createJDOMElement(roiDefinition));

        stringWriter = new StringWriter();
        newRoiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());
    }

    public void testWriteXmlWidthPath_AndInitFromJDOM() {
        final ROIDefinition roiDefinition = new ROIDefinition();
        GeneralPath path = new GeneralPath();
        path.moveTo(2f, 3f);
        path.lineTo(5f, 7f);
        path.quadTo(11f, 13f, 17f, 19f);
        path.curveTo(23f, 29f, 31f, 37f, 41f, 43f);
        path.closePath();
        roiDefinition.setShapeFigure(ShapeFigure.createPolygonArea(path, null));

        final String expexted = "" +
                                "<ROI_Definition>" + _ls +
                                "    <EXPRESSION />" + _ls +
                                "    <VALUE_RANGE_MAX>1.0</VALUE_RANGE_MAX>" + _ls +
                                "    <VALUE_RANGE_MIN>0.0</VALUE_RANGE_MIN>" + _ls +
                                "    <BITMASK_ENABLED>false</BITMASK_ENABLED>" + _ls +
                                "    <INVERTED>false</INVERTED>" + _ls +
                                "    <OR_COMBINED>false</OR_COMBINED>" + _ls +
                                "    <SHAPE_ENABLED>false</SHAPE_ENABLED>" + _ls +
                                "    <VALUE_RANGE_ENABLED>false</VALUE_RANGE_ENABLED>" + _ls +
                                "    <PIN_USE_ENABLED>false</PIN_USE_ENABLED>" + _ls +
                                "    <ROI_ONE_DIMENSIONS>false</ROI_ONE_DIMENSIONS>" + _ls +
                                "    <Shape_Figure type=\"Path\">" + _ls +
                                "        <SEGMENT type=\"moveTo\" value=\"2.0,3.0\" />" + _ls +
                                "        <SEGMENT type=\"lineTo\" value=\"5.0,7.0\" />" + _ls +
                                "        <SEGMENT type=\"quadTo\" value=\"11.0,13.0,17.0,19.0\" />" + _ls +
                                "        <SEGMENT type=\"cubicTo\" value=\"23.0,29.0,31.0,37.0,41.0,43.0\" />" + _ls +
                                "        <SEGMENT type=\"close\" />" + _ls +
                                "    </Shape_Figure>" + _ls +
                                "</ROI_Definition>";
        StringWriter stringWriter = new StringWriter();
        roiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());

        final ROIDefinition newRoiDefinition = new ROIDefinition();
        newRoiDefinition.initFromJDOMElem(DimapDocumentTest.createJDOMElement(roiDefinition));

        stringWriter = new StringWriter();
        newRoiDefinition.writeXML(new XmlWriter(stringWriter, false), 0);
        assertEquals(expexted, stringWriter.toString());
    }

    public void testInitFromJDOMElem() {
        final ROIDefinition roiDefinitionStart = new ROIDefinition();
        roiDefinitionStart.setBitmaskExpr("flags.Land");
        roiDefinitionStart.setBitmaskEnabled(true);
        roiDefinitionStart.setValueRangeMax(60f);
        roiDefinitionStart.setValueRangeMin(25.5f);
        roiDefinitionStart.setValueRangeEnabled(true);
        roiDefinitionStart.setPinUseEnabled(true);
        roiDefinitionStart.setInverted(true);
        roiDefinitionStart.setOrCombined(true);
        roiDefinitionStart.setShapeEnabled(true);
        final Element roiElement = DimapDocumentTest.createJDOMElement(roiDefinitionStart);

        final ROIDefinition roiDefinitionInit = new ROIDefinition();
        roiDefinitionInit.initFromJDOMElem(roiElement);

        assertTrue(roiDefinitionInit.isBitmaskEnabled());
        assertTrue(roiDefinitionInit.isInverted());
        assertTrue(roiDefinitionInit.isOrCombined());
        assertTrue(roiDefinitionInit.isShapeEnabled());
        assertTrue(roiDefinitionInit.isValueRangeEnabled());
        assertTrue(roiDefinitionInit.isPinUseEnabled());
        assertEquals(roiDefinitionStart.getBitmaskExpr(), roiDefinitionInit.getBitmaskExpr());
        assertEquals(roiDefinitionStart.getValueRangeMax(), roiDefinitionInit.getValueRangeMax(), 0.00001f);
        assertEquals(roiDefinitionStart.getValueRangeMin(), roiDefinitionInit.getValueRangeMin(), 0.00001f);
    }
}
