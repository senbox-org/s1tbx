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

import com.thoughtworks.xstream.io.xml.AbstractDocumentWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @since BEAM 4.6
 */
public class XStreamDomElementWriter extends AbstractDocumentWriter {

    public XStreamDomElementWriter(DomElement child) {
        this(child, new XmlFriendlyReplacer());
    }

    public XStreamDomElementWriter(DomElement child, XmlFriendlyReplacer xmlFriendlyReplacer) {
        super(child, xmlFriendlyReplacer);
    }

    @Override
    protected Object createNode(String name) {
        final DomElement top = top();
        if (top != null) {
            return top.createChild(escapeXmlName(name));
        }
        return new DefaultDomElement(escapeXmlName(name));
    }

    @Override
    public void addAttribute(String name, String value) {
        top().setAttribute(escapeXmlName(name), value);
    }

    @Override
    public void setValue(String text) {
        top().setValue(text);
    }

    private DomElement top() {
        return (DomElement) getCurrent();
    }
}