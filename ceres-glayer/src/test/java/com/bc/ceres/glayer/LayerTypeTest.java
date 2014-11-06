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

import com.bc.ceres.binding.PropertySet;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LayerTypeTest {

    @Test
    public void testLayerTypeDefaultNameAndAliasNames() {
        LayerType layerType = new LayerType() {
            @Override
            public boolean isValidFor(LayerContext ctx) {
                return false;
            }

            @Override
            public Layer createLayer(LayerContext ctx, PropertySet layerConfig) {
                return null;
            }

            @Override
            public PropertySet createLayerConfig(LayerContext ctx) {
                return null;
            }
        };

        assertEquals(layerType.getClass().getName(), layerType.getName());
        assertArrayEquals(new String[0], layerType.getAliases());
    }
}