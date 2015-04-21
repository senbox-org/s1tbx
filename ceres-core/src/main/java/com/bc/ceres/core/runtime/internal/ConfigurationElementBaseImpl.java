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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.runtime.ConfigurationElementBase;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;

import java.util.ArrayList;

/**
 * A configuration element, with its attributes and children,
 * directly reflects the content and structure of the extension
 * section within the declaring plug-in's manifest (plugin.xml) file.
 * <p>This interface also provides a way to create executable extension objects.
 * This interface is not intended to be implemented by clients.
 */
public abstract class ConfigurationElementBaseImpl<T extends ConfigurationElementBase>
        implements ConfigurationElementBase<T> {

    private final T parent;
    private final XppDom dom;
    private T[] children;

    protected ConfigurationElementBaseImpl(T parent, XppDom dom) {
        this.dom = dom;
        this.parent = parent;
    }

    XppDom getDom() {
        return dom;
    }

    public T getParent() {
        return parent;
    }

    public T getChild(String elementName) {
        T[] children = getChildren();
        // todo - naive implementation, maybe replace with something faster
        for (T child : children) {
            if (child.getName().equals(elementName)) {
                return (T) child;
            }
        }
        return null;
    }

    public T[] getChildren() {
        if (children == null) {
            children = createChildren(getDom().getChildren());
        }
        return children;
    }

    public T[] getChildren(String elementName) {
        T[] children = getChildren();
        ArrayList<T> list = new ArrayList<T>(children.length);
        // todo - naive implementation, maybe replace with something faster
        for (T child : children) {
            if (child.getName().equals(elementName)) {
                list.add(child);
            }
        }
        int n = list.size();
        return list.toArray(createEmptyArray(n));
    }

    public String getName() {
        return getDom().getName();
    }

    public String getValue() {
        return getDom().getValue();
    }

    public String getAttribute(String attributeName) {
        return getDom().getAttribute(attributeName);
    }

    public String[] getAttributeNames() {
        return getDom().getAttributeNames();
    }

    protected abstract T[] createChildren(XppDom[] doms);

    protected abstract T[] createEmptyArray(int n);
}
