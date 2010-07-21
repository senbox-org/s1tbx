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

package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.glayer.RasterImageLayerType;

import java.awt.geom.AffineTransform;

public class RasterImageLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public RasterImageLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(RasterImageLayerType.class));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertySet configuration = layerType.createLayerConfig(null);
        final Band raster = getProductManager().getProduct(0).getBandAt(0);
        configuration.setValue(RasterImageLayerType.PROPERTY_NAME_RASTER, raster);
        return layerType.createLayer(null, configuration);
    }
}