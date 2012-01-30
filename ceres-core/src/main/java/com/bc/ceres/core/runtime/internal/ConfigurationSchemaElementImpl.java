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

import com.bc.ceres.core.runtime.ConfigurationSchemaElement;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;

import java.util.HashSet;
import java.util.Set;

public class ConfigurationSchemaElementImpl extends ConfigurationElementBaseImpl<ConfigurationSchemaElement>
        implements ConfigurationSchemaElement {

    private ExtensionPointImpl declaringExtensionPoint;
    private XStream xStream;
    private Set<Class> classesWithConfiguredAliases;

    public ConfigurationSchemaElementImpl(ConfigurationSchemaElementImpl parent, XppDom dom) {
        super(parent, dom);
    }

    public ExtensionPointImpl getDeclaringExtensionPoint() {
        return declaringExtensionPoint;
    }

    @Override
    protected ConfigurationSchemaElement[] createChildren(XppDom[] doms) {
        ConfigurationSchemaElement[] children = createEmptyArray(doms.length);
        for (int i = 0; i < doms.length; i++) {
            ConfigurationSchemaElementImpl child = new ConfigurationSchemaElementImpl(this, doms[i]);
            child.setDeclaringExtensionPoint(declaringExtensionPoint);
            children[i] = child;
        }
        return children;
    }

    @Override
    protected ConfigurationSchemaElement[] createEmptyArray(int n) {
        return new ConfigurationSchemaElement[n];
    }

    void setDeclaringExtensionPoint(ExtensionPointImpl declaringExtensionPoint) {
        this.declaringExtensionPoint = declaringExtensionPoint;
    }

    XStream getXStream() {
        return xStream;
    }

    void setXStream(XStream xStream) {
        this.xStream = xStream;
    }

    void configureAliases(Class someClass) {
        if (classesWithConfiguredAliases == null) {
            classesWithConfiguredAliases = new HashSet<Class>(4);
        }
        if (!classesWithConfiguredAliases.contains(someClass)) {
            xStream.processAnnotations(someClass);
            classesWithConfiguredAliases.add(someClass);
        }
    }
}
