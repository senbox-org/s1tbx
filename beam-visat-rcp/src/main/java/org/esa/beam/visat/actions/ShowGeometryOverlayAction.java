package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayerFilterFactory;
import org.esa.beam.visat.VisatApp;

import java.util.List;

public class ShowGeometryOverlayAction extends AbstractShowOverlayAction {
    private final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();

    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            List<Layer> childLayers = getGeometryLayers(sceneView);
            for (Layer layer : childLayers) {
                layer.setVisible(isSelected());
            }
        }
    }

    @Override
    protected void updateEnableState(ProductSceneView view) {
        List<Layer> childLayers = getGeometryLayers(view);
        setEnabled(!childLayers.isEmpty());
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        List<Layer> childLayers = getGeometryLayers(view);
        boolean selected = false;
        for (Layer layer : childLayers) {
            if (layer.isVisible()) {
                selected = true;
                break;
            }
        }
        setSelected(selected);
    }

    private List<Layer> getGeometryLayers(ProductSceneView sceneView) {
        return LayerUtils.getChildLayers(sceneView.getRootLayer(), LayerUtils.SEARCH_DEEP, geometryFilter);
    }

}
