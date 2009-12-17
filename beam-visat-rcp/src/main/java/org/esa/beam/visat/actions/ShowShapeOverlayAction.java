package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.visat.VisatApp;

public class ShowShapeOverlayAction extends AbstractShowOverlayAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();
        final ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            final Layer layer = getSelectedVectorDataLayer(view);
            if (layer != null) {
                layer.setVisible(!layer.isVisible());
            }
        }
    }

    private static VectorDataLayer getSelectedVectorDataLayer(ProductSceneView sceneView) {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();
        if (node instanceof VectorDataNode) {
            final LayerFilter geometryNodeLayerFilter = new GeometryVectorDataNodeLayerFilter((VectorDataNode) node);
            final VectorDataLayer layer = (VectorDataLayer) LayerUtils.getChildLayer(sceneView.getRootLayer(),
                                                                                     geometryNodeLayerFilter,
                                                                                     LayerUtils.SearchMode.DEEP);
            if (layer != null) {
                return layer;
            }
        }
        return (VectorDataLayer) LayerUtils.getChildLayer(sceneView.getRootLayer(),
                                                          new GeometryLayerFilter(),
                                                          LayerUtils.SearchMode.DEEP);
    }

    @Override
    protected void updateEnableState(ProductSceneView view) {
        setEnabled(view != null && getSelectedVectorDataLayer(view) != null);
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        if (view != null) {
            final Layer layer = getSelectedVectorDataLayer(view);
            if (layer != null) {
                setSelected(layer.isVisible());
            }
        } else {
            setSelected(false);
        }
    }

    private static class GeometryLayerFilter implements LayerFilter {

        @Override
        public boolean accept(Layer layer) {
            if (layer instanceof VectorDataLayer) {
                final VectorDataLayer vectorLayer = (VectorDataLayer) layer;
                final String typeName = vectorLayer.getVectorDataNode().getFeatureType().getTypeName();
                return Product.GEOMETRY_FEATURE_TYPE_NAME.equals(typeName);
            }
            return false;
        }
    }

    private static class GeometryVectorDataNodeLayerFilter implements LayerFilter {

        private final VectorDataNode vectorDataNode;

        private GeometryVectorDataNodeLayerFilter(VectorDataNode vectorDataNode) {
            this.vectorDataNode = vectorDataNode;
        }

        @Override
        public boolean accept(Layer layer) {
            if (layer instanceof VectorDataLayer && ((VectorDataLayer) layer).getVectorDataNode() == vectorDataNode) {
                VectorDataLayer vecDataLayer = (VectorDataLayer) layer;
                final String typeName = vecDataLayer.getVectorDataNode().getFeatureType().getTypeName();
                return Product.GEOMETRY_FEATURE_TYPE_NAME.equals(typeName);
            }
            return false;
        }
    }
}
