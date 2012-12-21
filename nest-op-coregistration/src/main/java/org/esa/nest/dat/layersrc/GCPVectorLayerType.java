/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.layersrc;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

/**
 * The type descriptor of the {@link org.esa.nest.dat.layersrc.GCPVectorLayer}.
 *
 */
public class GCPVectorLayerType extends LayerType {

    public static GCPVectorLayer createLayer(final Product product, final Band band) {
        final LayerType type = LayerTypeRegistry.getLayerType(GCPVectorLayerType.class);
        final PropertySet template = type.createLayerConfig(null);
        template.setValue("product", product);
        template.setValue("band", band);
        return new GCPVectorLayer(template);
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        // todo - need to check for availability of windu, windv  (nf)
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        return new GCPVectorLayer(configuration);
    }

    // todo - rename getDefaultConfiguration  ? (nf)
    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
        final PropertyContainer valueContainer = new PropertyContainer();
        // todo - how do I know whether my value model type can be serialized or not? (nf)
        valueContainer.addProperty(new Property(new PropertyDescriptor("product", Product.class), new DefaultPropertyAccessor()));
        valueContainer.addProperty(new Property(new PropertyDescriptor("band", Band.class), new DefaultPropertyAccessor()));
        return valueContainer;
    }
}