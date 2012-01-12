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

package com.bc.ceres.glayer.annotations;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.junit.Test;

import static org.junit.Assert.*;

public class LayerTypeMetadataTest {

    @Test
    public void testAnnotatedLayerType() {
        final LayerType layerType = new AnnotatedLayerType();
        assertEquals("ALT", layerType.getName());
        assertArrayEquals(new String[]{"x", "y", "z"}, layerType.getAliases());
    }

    @Test
    public void testNonAnnotatedLayerType() {
        final LayerType layerType = new NonAnnotatedLayerType();
        assertEquals(layerType.getClass().getName(), layerType.getName());
        assertArrayEquals(new String[]{}, layerType.getAliases());
    }

    @Test
    public void testDerivedAnnotatedLayerType() {
        final LayerType layerType = new DerivedAnnotatedLayerType();
        assertEquals(layerType.getClass().getName(), layerType.getName());
        assertArrayEquals(new String[]{"u", "v"}, layerType.getAliases());
    }

    @Test
    public void testDerivedNonAnnotatedLayerType() {
        final LayerType layerType = new DerivedNonAnnotatedLayerType();
        assertEquals(layerType.getClass().getName(), layerType.getName());
        assertArrayEquals(new String[]{}, layerType.getAliases());
    }

    @LayerTypeMetadata(aliasNames = {"u", "v"})
    public static class DerivedAnnotatedLayerType extends AnnotatedLayerType {
    }

    public static class DerivedNonAnnotatedLayerType extends AnnotatedLayerType {
    }

    @LayerTypeMetadata(name = "ALT", aliasNames = {"x", "y", "z"})
    public static class AnnotatedLayerType extends LayerType {
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
    }

    public static class NonAnnotatedLayerType extends LayerType {
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
    }

}
