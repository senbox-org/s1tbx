package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.datamodel.Product;
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
        final Layer layer = sceneView.getSelectedLayer();
        if (layer instanceof VectorDataLayer) {
            final VectorDataLayer vectorLayer = (VectorDataLayer) layer;
            final String typeName = vectorLayer.getVectorDataNode().getFeatureType().getTypeName();
            if (Product.GEOMETRY_FEATURE_TYPE_NAME.equals(typeName)) {
                return vectorLayer;
            }
        }
        return null;
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
}
