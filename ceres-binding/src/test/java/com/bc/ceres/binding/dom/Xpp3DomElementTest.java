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

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

public class Xpp3DomElementTest extends AbstractDomElementTest {
    @Override
    protected DomElement createDomElement(String name) {
        return new Xpp3DomElement(new Xpp3Dom(name));
    }

    @Override
    protected DomElement createDomElement(String name, String value) {
        final Xpp3Dom xpp3Dom = new Xpp3Dom(name);
        xpp3Dom.setValue(value);
        return new Xpp3DomElement(xpp3Dom);
    }

    public void testConstructor() {
        try {
            new Xpp3DomElement((Xpp3Dom) null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
        try {
            new Xpp3DomElement((String) null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
    }

}