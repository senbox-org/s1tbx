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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glayer.RgbImageLayerType;

import java.awt.geom.AffineTransform;

public class RgbImageLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    public RgbImageLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(RgbImageLayerType.class));
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertySet configuration = layerType.createLayerConfig(null);

        final Product product = createTestProduct("Test", "TEST");
        addVirtualBand(product, "a", ProductData.TYPE_INT32, "17");
        addVirtualBand(product, "b", ProductData.TYPE_INT32, "11");
        addVirtualBand(product, "c", ProductData.TYPE_INT32, "67");
        getProductManager().addProduct(product);
        configuration.setValue("product", product);
        configuration.setValue("expressionR", "a + b");
        configuration.setValue("expressionG", "b + c");
        configuration.setValue("expressionB", "a - c");
        return layerType.createLayer(null, configuration);
    }
}
