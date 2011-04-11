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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;

public class ExtensionImpl implements Extension {

    public static final ExtensionImpl[] EMPTY_ARRAY = new ExtensionImpl[0];

    private final String point;
    private final ConfigurationElementImpl configurationElement;
    private final String id;

    private transient ModuleImpl declaringModule;
    private ExtensionPoint extensionPoint;

    public ExtensionImpl(String point, ConfigurationElementImpl configurationElement, String id) {
        Assert.notNull(point, "point");
        Assert.notNull(configurationElement, "configurationElement");
        this.point = point;
        this.configurationElement = configurationElement;
        this.configurationElement.setDeclaringExtension(this);
        this.id = id;
    }

    public String getPoint() {
        return point;
    }

    public String getId() {
        return id;
    }

    public ConfigurationElement getConfigurationElement() {
        lazilyResolveExtensionPoint();
        return configurationElement;
    }

    public Module getDeclaringModule() {
        return declaringModule;
    }

    void setDeclaringModule(ModuleImpl declaringModule) {
        this.declaringModule = declaringModule;
    }

    public ExtensionPoint getExtensionPoint() {
        lazilyResolveExtensionPoint();
        return extensionPoint;
    }

    private void lazilyResolveExtensionPoint() {
        if (extensionPoint == null) {
            ModuleRegistry registry = declaringModule.getRegistry();
            if (registry != null) {
                extensionPoint = registry.getExtensionPoint(point, declaringModule);
                if (extensionPoint != null) {
                    // Set root of all schema elements
                    configurationElement.setSchemaElement(
                            (ConfigurationSchemaElementImpl) extensionPoint.getConfigurationSchemaElement());
                }
            }
        }
    }
}

