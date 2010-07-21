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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;


/**
 * A layer which can contain other layers.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class CollectionLayer extends Layer {

    public CollectionLayer() {
        this("Collection Layer");
    }

    public CollectionLayer(String name) {
        this(type(), type().createLayerConfig(null), name);
    }

    public CollectionLayer(Type type, PropertySet configuration, String name) {
        super(type, configuration);
        setName(name);
    }

    @Override
    public boolean isCollectionLayer() {
        return true;
    }

    private static Type type() {
        return LayerTypeRegistry.getLayerType(Type.class);
    }

    public static class Type extends LayerType {
        
        private static final String TYPE_NAME = "CollectionLayerType";
        private static final String[] ALIASES = {"com.bc.ceres.glayer.CollectionLayer$Type"};

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
            return true;
        }

        @Override
        public PropertySet createLayerConfig(LayerContext ctx) {
            return new PropertyContainer();
        }

        @Override
        public Layer createLayer(LayerContext ctx, PropertySet configuration) {
            return new CollectionLayer(this, configuration, "Collection Layer");
        }
    }

}
