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

package com.bc.ceres.binding.dom;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashSet;

public abstract class AbstractDomElementTest extends TestCase {

    protected abstract DomElement createDomElement(String name);

    protected abstract DomElement createDomElement(String name, String value);

    public void testSingleElement() {
        DomElement element = createDomElement("x", "8");
        assertEquals("x", element.getName());
        assertEquals("8", element.getValue());
        assertEquals("<x>8</x>", element.toXml());

        element = createDomElement("x", null);
        assertEquals("x", element.getName());
        assertEquals(null, element.getValue());
        assertEquals("<x/>", element.toXml());

        final String name = "x";
        element = createDomElement(name);
        assertEquals("x", element.getName());
        assertEquals(null, element.getValue());
        assertEquals("<x/>", element.toXml());
    }


    public void testAttribues() {
        DomElement element = createDomElement("point");
        assertNotNull(element.getAttributeNames());
        assertEquals(0, element.getAttributeNames().length);

        element.setAttribute("x", "56");
        element.setAttribute("y", "24");
        element.setAttribute("z", "98");

        assertNotNull(element.getAttributeNames());

        final HashSet<String> names = new HashSet<String>(Arrays.asList(element.getAttributeNames()));
        assertEquals(3, names.size());
        assertTrue(names.contains("x"));
        assertTrue(names.contains("y"));
        assertTrue(names.contains("z"));

        assertEquals("56", element.getAttribute("x"));
        assertEquals("24", element.getAttribute("y"));
        assertEquals("98", element.getAttribute("z"));
        assertEquals(null, element.getAttribute("w"));

        String xml = element.toXml();
        assertTrue(xml.startsWith("<point "));
        assertTrue(xml.contains("x=\"56\""));
        assertTrue(xml.contains("y=\"24\""));
        assertTrue(xml.contains("z=\"98\""));
        assertTrue(xml.endsWith("/>"));
    }

    public void testChildren() {
        DomElement element = createDomElement("point");
        assertEquals(0, element.getChildCount());

        element.createChild("x").setValue("56");
        element.addChild(createDomElement("y", "24"));
        element.addChild(createDomElement("z", "98"));

        assertNull(element.getParent());
        assertNotNull(element.getChildCount());
        assertEquals(3, element.getChildCount());

        assertNotNull(element.getChild("x"));
        assertNotNull(element.getChild("y"));
        assertNotNull(element.getChild("z"));
        assertNull(element.getChild("w"));

        assertEquals("56", element.getChild("x").getValue());
        assertEquals("24", element.getChild("y").getValue());
        assertEquals("98", element.getChild("z").getValue());

        assertSame(element, element.getChild("x").getParent());
        assertSame(element, element.getChild("y").getParent());
        assertSame(element, element.getChild("z").getParent());

        assertEquals("" +
                "<point>\n" +
                "<x>56</x>\n" +
                "<y>24</y>\n" +
                "<z>98</z>\n" +
                "</point>",
                     element.toXml().replace("  ", ""));
    }

    public void testToXmlWithAttributesAndChildren() {
        DomElement element = createDomElement("layer");
        element.setAttribute("id", "a62b98ff5");
        element.createChild("name").setValue("ROI");
        element.createChild("visible").setValue("true");
        element.createChild("configuration");
        element.getChild("configuration").createChild("outlineColor").setValue("23,45,230");
        element.getChild("configuration").createChild("fillColor").setValue("123, 64,30");
        element.getChild("configuration").createChild("transparency").setValue("0.6");
        String xml = element.toXml();
        //System.out.println("xml = " + xml);

        assertEquals("" +
                "<layer id=\"a62b98ff5\">\n" +
                "<name>ROI</name>\n" +
                "<visible>true</visible>\n" +
                "<configuration>\n" +
                "<outlineColor>23,45,230</outlineColor>\n" +
                "<fillColor>123, 64,30</fillColor>\n" +
                "<transparency>0.6</transparency>\n" +
                "</configuration>\n" +
                "</layer>",
                     xml.replace("  ", ""));
    }

}