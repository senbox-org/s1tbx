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

package com.bc.ceres.glayer;

import com.bc.ceres.glayer.support.ImageLayer;
import junit.framework.TestCase;

import java.util.Set;

public class LayerTypeRegistryTest extends TestCase {

    public void testGetLayerTypeByClass() {
        assertNotNull(LayerTypeRegistry.getLayerType(CollectionLayer.Type.class));
        assertNotNull(LayerTypeRegistry.getLayerType(ImageLayer.Type.class));
    }

    public void testGetLayerTypeByName() {
        assertNotNull(LayerTypeRegistry.getLayerType(CollectionLayer.Type.class.getName()));
        assertNotNull(LayerTypeRegistry.getLayerType(ImageLayer.Type.class.getName()));
    }

    public void testLayerTypeIsOfCorrectType() {
        LayerType collectionLayerType = LayerTypeRegistry.getLayerType(CollectionLayer.Type.class.getName());
        assertTrue(collectionLayerType instanceof CollectionLayer.Type);
        LayerType imageLayerType = LayerTypeRegistry.getLayerType(ImageLayer.Type.class.getName());
        assertTrue(imageLayerType instanceof ImageLayer.Type);
    }

    public void testAliases() {
        LayerType imageLayerType = LayerTypeRegistry.getLayerType("ImageLayerType");
        assertTrue(imageLayerType instanceof ImageLayer.Type);
    }

    public void testGetLayerTypes() {
        Set<LayerType> layerTypeSet1 = LayerTypeRegistry.getLayerTypes();
        assertNotNull(layerTypeSet1);
        assertTrue(layerTypeSet1.size() > 0);

        Set<LayerType> layerTypeSet2 = LayerTypeRegistry.getLayerTypes();
        assertNotSame(layerTypeSet1, layerTypeSet2);
    }
}
