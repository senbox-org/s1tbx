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

public class DefaultDomElementTest extends AbstractDomElementTest {

    @Override
    protected DomElement createDomElement(String name) {
        return new DefaultDomElement(name);
    }

    @Override
    protected DomElement createDomElement(String name, String value) {
        return new DefaultDomElement(name, value);
    }

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
        try {
            new DefaultDomElement("color", null);
            // ok
        } catch (NullPointerException e) {
            fail("NPE not expected");
        }
    }

    public void testMixChildren() {
        testMixChildren(new DefaultDomElement("a"), new DefaultDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new DefaultDomElement("b"), new XppDomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new XppDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new DefaultDomElement("a"), new XppDomElement("b"), new XppDomElement("c"));
        testMixChildren(new XppDomElement("a"), new DefaultDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new XppDomElement("a"), new DefaultDomElement("b"), new XppDomElement("c"));
        testMixChildren(new XppDomElement("a"), new XppDomElement("b"), new DefaultDomElement("c"));
        testMixChildren(new XppDomElement("a"), new XppDomElement("b"), new XppDomElement("c"));
    }

    private void testMixChildren(DomElement a, DomElement b, DomElement c) {
        b.addChild(c);
        a.addChild(b);

        assertNotNull(a.getChild("b"));
        assertNotNull(a.getChild("b").getChild("c"));

        assertNull(a.getParent());
        assertSame(a, a.getChild("b").getParent());
        assertSame(a.getChild("b"), a.getChild("b").getChild("c").getParent());

        assertEquals("<a>\n" +
                             "<b>\n" +
                             "<c/>\n" +
                             "</b>\n" +
                             "</a>", a.toXml().replace("  ", ""));
    }
}
