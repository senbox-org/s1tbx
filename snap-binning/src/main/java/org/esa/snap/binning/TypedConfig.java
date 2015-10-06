/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ExtensibleObject;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.util.ObjectUtils;

/**
 * Configuration of a typed object.
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 *
 * @see TypedDescriptor
 */
public class TypedConfig extends ExtensibleObject {
    @Parameter(alias = "type")
    protected String type;

    private transient PropertySet propertySet;

    public TypedConfig() {
    }

    public TypedConfig(String typeName) {
        this.type = typeName;
    }

    // used on Calvalus
    public PropertySet asPropertySet() {
        if (propertySet == null) {
            propertySet = PropertyContainer.createObjectBacked(this, new ParameterDescriptorFactory());
        }
        return propertySet;
    }

    public String getName() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypedConfig)) return false;

        TypedConfig that = (TypedConfig) o;

        PropertySet ps1 = asPropertySet();
        PropertySet ps2 = that.asPropertySet();
        Property[] properties1 = ps1.getProperties();
        Property[] properties2 = ps2.getProperties();
        if (properties1.length != properties2.length) {
            return false;
        }
        for (Property p1 : properties1) {
            Object v1 = p1.getValue();
            Object v2 = ps2.getValue(p1.getName());
            if (!ObjectUtils.equalObjects(v1, v2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int code = 0;
        PropertySet ps = asPropertySet();
        for (Property property : ps.getProperties()) {
            String name = property.getName();
            Object value = property.getValue();
            code = 31 * code + name.hashCode();
            code = 31 * code + (value != null ? value.hashCode() : 0);
        }
        return code;
    }
}
