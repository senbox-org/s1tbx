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

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VectorDataNode;

/**
 * Filter out layers of type {@link VectorDataLayer}.
 */
public class VectorDataLayerFilterFactory {
    public static LayerFilter createGeometryFilter() {
        return new TypeNameFilter(Product.GEOMETRY_FEATURE_TYPE_NAME);
    }

    public static LayerFilter createNodeFilter(VectorDataNode vectorDataNode) {
        return new NodeFilter(vectorDataNode);
    }

    private static class TypeNameFilter implements LayerFilter {

        private final String featureTypeName;

        private TypeNameFilter(String featureTypeName) {
            this.featureTypeName = featureTypeName;
        }

        @Override
        public boolean accept(Layer layer) {
            return layer instanceof VectorDataLayer
                    && featureTypeName.equals(((VectorDataLayer) layer).getVectorDataNode().getFeatureType().getTypeName());
        }
    }

    private static class NodeFilter implements LayerFilter {

        private final VectorDataNode vectorDataNode;

        private NodeFilter(VectorDataNode vectorDataNode) {
            this.vectorDataNode = vectorDataNode;
        }

        @Override
        public boolean accept(Layer layer) {
            return layer instanceof VectorDataLayer
                    && (((VectorDataLayer) layer).getVectorDataNode() == vectorDataNode);
        }
    }
}
