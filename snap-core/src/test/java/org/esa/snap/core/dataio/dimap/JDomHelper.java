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
package org.esa.snap.core.dataio.dimap;

import org.jdom.Element;

public class JDomHelper {

    public static void addElement(String tag, long value, Element element) {
        addElement(tag, String.valueOf(value), element);
    }

    public static void addElement(String tag, float value, Element element) {
        addElement(tag, String.valueOf(value), element);
    }

    public static void addElement(String tag, double value, Element element) {
        addElement(tag, String.valueOf(value), element);
    }

    public static void addElement(String tag, boolean value, Element element) {
        addElement(tag, String.valueOf(value), element);
    }

    public static void addElement(String tag, String content, Element element) {
        element.addContent(createElement(tag, content));
    }

    public static Element createElement(String tag, String content) {
        Element element = new Element(tag);
        element.addContent(content);
        return element;
    }

}
