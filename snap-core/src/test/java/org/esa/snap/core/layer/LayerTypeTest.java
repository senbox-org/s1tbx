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

package org.esa.snap.core.layer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class LayerTypeTest {

    private final Class<? extends LayerType> layerTypeClass;

    protected LayerTypeTest(Class<? extends LayerType> layerTypeClass) {
        this.layerTypeClass = layerTypeClass;
    }

    public Class<? extends LayerType> getLayerTypeClass() {
        return layerTypeClass;
    }

    public LayerType getLayerType() {
        return LayerTypeRegistry.getLayerType(getLayerTypeClass());
    }

    @Test
    public void testLayerType() {
        final LayerType layerType = getLayerType();


        assertNotNull(layerType);
        assertTrue(layerTypeClass.isAssignableFrom(layerType.getClass()));
    }

    @Test
    public void testCreateLayerWithNullConfiguration() {
        final LayerType layerType = getLayerType();

        try {
            layerType.createLayer(null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    protected static void ensurePropertyIsDefined(PropertySet template, String name, Class<?> type) {
        final Property property = template.getProperty(name);
        assertNotNull(property);

        final PropertyDescriptor descriptor = property.getDescriptor();
        assertNotNull(descriptor);
        assertEquals(type, descriptor.getType());
        assertNotNull(descriptor.getDefaultValue());
        assertNotNull(property.getValue());
    }

    protected static void ensurePropertyIsDeclaredButNotDefined(PropertySet template, String name, Class<?> type) {
        final Property property = template.getProperty(name);
        assertNotNull(property);

        final PropertyDescriptor descriptor = property.getDescriptor();
        assertNotNull(descriptor);
        assertEquals(type, descriptor.getType());
        assertNull(descriptor.getDefaultValue());
        assertNull(property.getValue());
    }
}
