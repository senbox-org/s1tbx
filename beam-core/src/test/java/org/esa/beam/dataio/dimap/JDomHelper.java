/*
 * $Id: JDomHelper.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.dimap;

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
