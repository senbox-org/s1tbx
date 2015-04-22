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

import com.thoughtworks.xstream.io.xml.AbstractDocumentReader;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @since BEAM 4.6
 */
public class XStreamDomElementReader extends AbstractDocumentReader {

    private DomElement current;
    private String[] attributeNames;

    public XStreamDomElementReader(DomElement child) {
        this(child, new XmlFriendlyReplacer());
    }

    public XStreamDomElementReader(DomElement child, XmlFriendlyReplacer xmlFriendlyReplacer) {
        super(child, xmlFriendlyReplacer);
    }

    @Override
    protected void reassignCurrentElement(Object o) {
        current = (DomElement) o;
        attributeNames = current.getAttributeNames();
    }

    @Override
    protected Object getParent() {
        return current.getParent();
    }

    @Override
    protected Object getChild(int i) {
        return current.getChild(i);
    }

    @Override
    protected int getChildCount() {
        return current.getChildCount();
    }

    @Override
    public String getNodeName() {
        return current.getName();
    }

    @Override
    public String getValue() {
        return current.getValue();
    }

    @Override
    public String getAttribute(String s) {
        return current.getAttribute(s);
    }

    @Override
    public String getAttribute(int i) {
        return current.getAttribute(attributeNames[i]);
    }

    @Override
    public int getAttributeCount() {
        return attributeNames.length;
    }

    @Override
    public String getAttributeName(int i) {
        return attributeNames[i];
    }
}
