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

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.glayer.ProductLayerContext;

public class VectorDataCollectionLayerType extends CollectionLayer.Type {

    private static final String TYPE_NAME = "VectorDataCollectionLayerType";
    private static final String[] ALIASES = {"org.esa.beam.framework.ui.product.VectorDataCollectionLayerType"};

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
        final ProductNodeGroup<VectorDataNode> vectorDataGroup = plc.getProduct().getVectorDataGroup();

        return new VectorDataCollectionLayer(this, vectorDataGroup, configuration);
    }
}
