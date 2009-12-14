package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.visat.VisatApp;

public class ShowShapeOverlayAction extends AbstractShowOverlayAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final Layer layer = getGeometryLayer(view.getRootLayer());
            if (layer != null) {
                layer.setVisible(!layer.isVisible());
            }
        }
    }

    private Layer getGeometryLayer(Layer rootLayer) {
        return LayerUtils.getChildLayer(rootLayer, new GeometryLayerFilter(), LayerUtils.SearchMode.DEEP);
    }

    @Override
    protected void updateEnableState(ProductSceneView view) {
        setEnabled(view != null && getGeometryLayer(view.getRootLayer()) != null);
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        if (view != null) {
            final Layer layer = getGeometryLayer(view.getRootLayer());
            if (layer != null) {
                setSelected(layer.isVisible());
            }
        }
        setSelected(false);

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
}
