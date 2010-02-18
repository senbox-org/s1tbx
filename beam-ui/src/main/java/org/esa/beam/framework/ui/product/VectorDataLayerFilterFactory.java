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
