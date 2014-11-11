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
import com.bc.ceres.core.runtime.ConfigurationSchemaElement;

public class ExtensionPointImpl implements ExtensionPoint {

    public static final ExtensionPointImpl[] EMPTY_ARRAY = new ExtensionPointImpl[0];

    private final String id;
    private final ConfigurationSchemaElementImpl configurationSchemaElement;

    private transient String qualifiedId;
    private transient ModuleImpl declaringModule;

    public ExtensionPointImpl(String id, ConfigurationSchemaElementImpl configurationSchemaElement) {
        Assert.notNull(id, "id");
        Assert.notNull(configurationSchemaElement, "configurationSchemaElement");
        this.id = id;
        this.configurationSchemaElement = configurationSchemaElement;
        this.configurationSchemaElement.setDeclaringExtensionPoint(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getQualifiedId() {
        return qualifiedId;
    }

    @Override
    public Module getDeclaringModule() {
        return declaringModule;
    }

    void setDeclaringModule(ModuleImpl declaringModule) {
        this.declaringModule = declaringModule;
        this.qualifiedId = declaringModule.getSymbolicName() + ':' + id;
    }

    public Extension getExtension(String extensionId) {
        Extension[] extensions = getExtensions();
        for (Extension extension : extensions) {
            if (extensionId.equals(extension.getId())) {
                return extension;
            }
        }
        return null;
    }

    @Override
    public Extension[] getExtensions() {
        ModuleRegistry registry = declaringModule.getRegistry();
        return registry != null ? registry.getExtensions(qualifiedId) : null;
    }

    @Override
    public ConfigurationElement[] getConfigurationElements() {
        Extension[] extensions = getExtensions();
        if (extensions == null) {
            return null;
        }
        ConfigurationElement[] configurationElements = new ConfigurationElement[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            configurationElements[i] = extensions[i].getConfigurationElement();
        }
        return configurationElements;
    }

    @Override
    public ConfigurationSchemaElement getConfigurationSchemaElement() {
        return configurationSchemaElement;
    }
}
