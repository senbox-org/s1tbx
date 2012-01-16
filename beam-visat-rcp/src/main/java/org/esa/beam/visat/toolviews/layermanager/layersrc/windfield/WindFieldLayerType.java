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

package org.esa.beam.visat.toolviews.layermanager.layersrc.windfield;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import org.esa.beam.framework.datamodel.RasterDataNode;

/**
 * The type descriptor of the {@link WindFieldLayer}.
 *
 * @author Norman Fomferra
 * @since BEAM 4.6
 */
@LayerTypeMetadata(name = "WindFieldLayerType",
                   aliasNames = {"org.esa.beam.visat.toolviews.layermanager.layersrc.windfield.WindFieldLayerType"})
public class WindFieldLayerType extends LayerType {

    public static WindFieldLayer createLayer(RasterDataNode windu, RasterDataNode windv) {
        LayerType type = LayerTypeRegistry.getLayerType(WindFieldLayerType.class);
        final PropertySet template = type.createLayerConfig(null);
        template.setValue("windu", windu);
        template.setValue("windv", windv);
        return new WindFieldLayer(type, windu, windv, template);
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        // todo - need to check for availability of windu, windv  (nf)
        return true;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        final RasterDataNode windu = (RasterDataNode) configuration.getValue("windu");
        final RasterDataNode windv = (RasterDataNode) configuration.getValue("windv");
        return new WindFieldLayer(this, windu, windv, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer propertyContainer = new PropertyContainer();
        // todo - how do I know whether my value model type can be serialized or not? (nf)
        propertyContainer.addProperty(new Property(new PropertyDescriptor("windu", RasterDataNode.class), new DefaultPropertyAccessor()));
        propertyContainer.addProperty(new Property(new PropertyDescriptor("windv", RasterDataNode.class), new DefaultPropertyAccessor()));
        return propertyContainer;
    }
}
