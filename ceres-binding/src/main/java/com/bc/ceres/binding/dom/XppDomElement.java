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

import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;

import java.io.StringWriter;
import java.util.HashMap;


public class XppDomElement implements DomElement {
    private final XppDom xppDom;
    private DomElement parent;
    private HashMap<XppDom, DomElement> children;

    public XppDomElement(String name) {
        this(new XppDom(name));
    }

    public XppDomElement(XppDom xppDom) {
        Assert.notNull(xppDom, "xppDom");
        Assert.notNull(xppDom.getName(), "xppDom.getName()");
        this.xppDom = xppDom;
    }

    public XppDom getXppDom() {
        return xppDom;
    }

    @Override
    public String getName() {
        return xppDom.getName();
    }

    @Override
    public String getValue() {
        return xppDom.getValue();
    }

    @Override
    public void setValue(String value) {
        xppDom.setValue(value);
    }

    @Override
    public DomElement getParent() {
        return parent;
    }

    @Override
    public void setParent(DomElement parent) {
        if (parent instanceof XppDomElement) {
            XppDomElement xppDomElement = (XppDomElement) parent;
            xppDom.setParent(xppDomElement.getXppDom());
        }
        this.parent = parent;
    }

    @Override
    public DomElement[] getChildren() {
        return getChildren(xppDom.getChildren());
    }

    @Override
    public DomElement[] getChildren(String elementName) {
        return getChildren(xppDom.getChildren(elementName));
    }

    @Override
    public DomElement getChild(int index) {
        XppDom child = xppDom.getChild(index);
        if (child == null) {
            return null;
        }
        return getChild(child);
    }

    @Override
    public DomElement getChild(String elementName) {
        final XppDom child = xppDom.getChild(elementName);
        if (child == null) {
            return null;
        }
        return getChild(child);
    }

    @Override
    public int getChildCount() {
        return xppDom.getChildCount();
    }

    @Override
    public DomElement createChild(String name) {
        final XppDomElement child = new XppDomElement(new XppDom(name));
        addChild(child);
        return child;
    }

    @Override
    public void addChild(DomElement child) {
        final XppDom dom;
        if (child instanceof XppDomElement) {
            dom = ((XppDomElement) child).getXppDom();
        } else {
            dom = toXppDom(child);
        }
        xppDom.addChild(dom);
        child.setParent(this);
    }

    @Override
    public String[] getAttributeNames() {
        return xppDom.getAttributeNames();
    }

    @Override
    public String getAttribute(String attributeName) {
        return xppDom.getAttribute(attributeName);
    }

    @Override
    public void setAttribute(String name, String value) {
        xppDom.setAttribute(name, value);
    }

    @Override
    public String toXml() {
        final StringWriter writer = new StringWriter();
        new HierarchicalStreamCopier().copy(new XppDomReader(xppDom), new PrettyPrintWriter(writer));
        return writer.toString();
    }

    private XppDom toXppDom(DomElement domElement) {
        XppDom xppDom = new XppDom(domElement.getName());
        if (domElement.getValue() != null) {
            xppDom.setValue(domElement.getValue());
        }
        String[] names = domElement.getAttributeNames();
        for (String name : names) {
            xppDom.setAttribute(name, domElement.getAttribute(name));
        }
        DomElement[] children = domElement.getChildren();
        for (DomElement child : children) {
            xppDom.addChild(toXppDom(child));
        }
        return xppDom;
    }

    private DomElement[] getChildren(XppDom[] xppChildren) {
        final DomElement[] domElements = new XppDomElement[xppChildren.length];
        for (int i = 0; i < xppChildren.length; i++) {
            domElements[i] = getChild(xppChildren[i]);
        }
        return domElements;
    }

    private DomElement getChild(XppDom child) {
        if (children != null) {
            DomElement childElement = children.get(child);
            if (childElement != null) {
                return childElement;
            }
        } else {
            children = new HashMap<XppDom, DomElement>();
        }

        DomElement childElement = new XppDomElement(child);
        childElement.setParent(this);

        children.put(child, childElement);

        return childElement;
    }

}
