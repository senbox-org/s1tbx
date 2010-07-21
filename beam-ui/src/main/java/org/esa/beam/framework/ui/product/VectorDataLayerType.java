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

package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.glayer.ProductLayerContext;

/**
 * @author Marco Peters
 * @author Ralf Quast
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class VectorDataLayerType extends LayerType {

    public static final String PROPERTY_NAME_VECTOR_DATA = "vectorData";
    public static final String VECTOR_DATA_LAYER_ID_PREFIX = "org.esa.beam.layers.vectorData";

    private static final String TYPE_NAME = "VectorDataLayerType";
    private static final String[] ALIASES = {"org.esa.beam.framework.ui.product.VectorDataLayerType"};
    
    private static int id;

    public static Layer createLayer(VectorDataNode vectorDataNode) {
        final VectorDataLayerType layerType = LayerTypeRegistry.getLayerType(VectorDataLayerType.class);
        final PropertySet configuration = layerType.createLayerConfig(null);
        configuration.setValue(PROPERTY_NAME_VECTOR_DATA, vectorDataNode.getName());

        return layerType.createLayer(vectorDataNode, configuration);
    }

    @Override
    public String getName() {
        return TYPE_NAME;
    }
    
    @Override
    public String[] getAliases() {
        return ALIASES;
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return ctx instanceof ProductLayerContext;
    }

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        Assert.notNull(ctx, "ctx");
        final ProductLayerContext plc = (ProductLayerContext) ctx;
        final String vectorDataName = (String) configuration.getValue(PROPERTY_NAME_VECTOR_DATA);
        final VectorDataNode vectorDataNode = plc.getProduct().getVectorDataGroup().get(vectorDataName);

        return createLayer(vectorDataNode, configuration);
    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer configuration = new PropertyContainer();
        configuration.addProperty(Property.create(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA, String.class));
        return configuration;
    }

    private Layer createLayer(VectorDataNode vectorDataNode, PropertySet configuration) {
        final VectorDataLayer layer = new VectorDataLayer(this, vectorDataNode, configuration);
        layer.setId(VECTOR_DATA_LAYER_ID_PREFIX + (++id));

        return layer;
    }
}