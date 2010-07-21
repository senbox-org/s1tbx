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

import java.util.ArrayList;
import java.util.HashMap;


public class DefaultDomElement implements DomElement {

    private DomElement parent;
    private String name;
    private String value;
    private ArrayList<String> attributeList;
    private HashMap<String, String> attributeMap;
    private ArrayList<DomElement> elementList;
    private HashMap<String, DomElement> elementMap;


    public DefaultDomElement(String name) {
        this(name, null);
    }

    public DefaultDomElement(String name, String value) {
        Assert.notNull(name, "name");
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public DomElement getParent() {
        return parent;
    }

    @Override
    public void setParent(DomElement parent) {
        this.parent = parent;
    }

    @Override
    public String getAttribute(String name) {
        Assert.notNull(name, "name");
        return attributeMap != null ? attributeMap.get(name) : null;
    }

    @Override
    public void setAttribute(String name, String value) {
        Assert.notNull(name, "name");
        Assert.notNull(value, "value");
        if (attributeList == null) {
            attributeList = new ArrayList<String>();
            attributeMap = new HashMap<String, String>();
        }
        if (!attributeMap.containsKey(name)) {
            attributeList.add(name);
        }
        attributeMap.put(name, value);
    }

    @Override
    public String[] getAttributeNames() {
        return attributeList != null ? attributeList.toArray(new String[attributeList.size()]) : new String[0];
    }

    @Override
    public DomElement getChild(String elementName) {
        return elementMap != null ? elementMap.get(elementName) : null;
    }

    @Override
    public DomElement[] getChildren() {
        return elementList != null ? elementList.toArray(new DomElement[elementList.size()]) : new DomElement[0];
    }

    @Override
    public DomElement[] getChildren(String elementName) {
        if (elementList == null) {
            return new DomElement[0];
        }
        ArrayList<DomElement> children = new ArrayList<DomElement>(elementList.size());
        for (DomElement domElement : elementList) {
            if (elementName.equals(domElement.getName())) {
                children.add(domElement);
            }
        }
        return children.toArray(new DomElement[children.size()]);
    }

    @Override
    public DomElement getChild(int index) {
        return elementList != null ? elementList.get(index) : null;
    }

    @Override
    public int getChildCount() {
        return elementList != null ? elementList.size() : 0;
    }

    @Override
    public DomElement createChild(String name) {
        final DefaultDomElement child = new DefaultDomElement(name);
        addChild(child);
        return child;
    }

    @Override
    public void addChild(DomElement child) {
        if (elementList == null) {
            this.elementList = new ArrayList<DomElement>();
            this.elementMap = new HashMap<String, DomElement>();
        }
        elementList.add(child);
        elementMap.put(child.getName(), child);
        child.setParent(this);
    }

    @Override
    public String toXml() {
        StringBuilder builder = new StringBuilder(256);

        builder.append("<");
        builder.append(getName());
        if (attributeList != null) {
            for (String name1 : attributeList) {
                builder.append(' ');
                builder.append(name1);
                builder.append('=');
                builder.append('"');
                builder.append(attributeMap.get(name1));
                builder.append('"');
            }
        }

        if (elementList != null) {
            builder.append(">");
            builder.append('\n');
            if (getValue() != null) {
                builder.append(getValue());
                builder.append('\n');
            }
            for (DomElement element : elementList) {
                for (String line : element.toXml().split("\\n")) {
                    builder.append("    ");
                    builder.append(line);
                    builder.append("\n");
                }
            }
            builder.append("</");
            builder.append(getName());
            builder.append(">");
        } else {
            if (getValue() != null) {
                builder.append(">");
                builder.append(getValue());
                builder.append("</");
                builder.append(getName());
            } else {
                builder.append("/");
            }
            builder.append(">");
        }

        return builder.toString();
    }

}