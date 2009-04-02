package com.bc.ceres.binding.dom;

import junit.framework.TestCase;

public class DefaultDomElementTest extends TestCase {
    public void testConstructor() {
        try {
            new DefaultDomElement(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new DefaultDomElement(null, "red");
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }

        DefaultDomElement element = new DefaultDomElement("x", "8");
        assertEquals("x", element.getName());
        assertEquals("8", element.getValue());
        assertEquals("<x>8</x>", element.toXml());

        element = new DefaultDomElement("x", null);
        assertEquals("x", element.getName());
        assertEquals(null, element.getValue());
        assertEquals("<x/>", element.toXml());

        element = new DefaultDomElement("x");
        assertEquals("x", element.getName());
        assertEquals(null, element.getValue());
        assertEquals("<x/>", element.toXml());
    }

    public void testAttribues() {
        DefaultDomElement element = new DefaultDomElement("point");
        assertNotNull(element.getAttributeNames());
        assertEquals(0, element.getAttributeNames().length);

        element.setAttribute("x", "56");
        element.setAttribute("y", "24");
        element.setAttribute("z", "98");

        assertNotNull(element.getAttributeNames());
        assertEquals(3, element.getAttributeNames().length);
        assertEquals("x", element.getAttributeNames()[0]);
        assertEquals("y", element.getAttributeNames()[1]);
        assertEquals("z", element.getAttributeNames()[2]);

        assertEquals("56", element.getAttribute("x"));
        assertEquals("24", element.getAttribute("y"));
        assertEquals("98", element.getAttribute("z"));
        assertEquals(null, element.getAttribute("w"));

        assertEquals("<point x=\"56\" y=\"24\" z=\"98\"/>", element.toXml());
    }

    public void testChildren() {
        DefaultDomElement element = new DefaultDomElement("point");
        assertEquals(0, element.getChildCount());

        element.createChild("x").setValue("56");
        element.addChild(new DefaultDomElement("y", "24"));
        element.addChild(new DefaultDomElement("z", "98"));

        assertNull(element.getParent());
        assertNotNull(element.getChildCount());
        assertEquals(3, element.getChildCount());

        assertNotNull(element.getChild("x"));
        assertNotNull(element.getChild("y"));
        assertNotNull(element.getChild("z"));
        assertNull(element.getChild("w"));

        assertSame(element, element.getChild("x").getParent());
        assertSame(element, element.getChild("y").getParent());
        assertSame(element, element.getChild("z").getParent());

        assertEquals("56", element.getChild("x").getValue());
        assertEquals("24", element.getChild("y").getValue());
        assertEquals("98", element.getChild("z").getValue());

        assertEquals("" +
                "<point>\n" +
                "    <x>56</x>\n" +
                "    <y>24</y>\n" +
                "    <z>98</z>\n" +
                "</point>",
                     element.toXml());
    }

    public void testToXmlWithAttributesAndChildren() {
        DefaultDomElement element = new DefaultDomElement("layer");
        element.setAttribute("id", "a62b98ff5");
        element.createChild("name").setValue("ROI");
        element.createChild("visible").setValue("true");
        element.createChild("configuration");
        element.getChild("configuration").createChild("outlineColor").setValue("23,45,230");
        element.getChild("configuration").createChild("fillColor").setValue("123, 64,30");
        element.getChild("configuration").createChild("transparency").setValue("0.6");
        String xml = element.toXml();
        System.out.println("xml = " + xml);

        assertEquals("" +
                "<layer id=\"a62b98ff5\">\n" +
                "    <name>ROI</name>\n" +
                "    <visible>true</visible>\n" +
                "    <configuration>\n" +
                "        <outlineColor>23,45,230</outlineColor>\n" +
                "        <fillColor>123, 64,30</fillColor>\n" +
                "        <transparency>0.6</transparency>\n" +
                "    </configuration>\n" +
                "</layer>",
                     xml);
    }

}
