package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.mpage.MultiPagePane;
import org.esa.beam.framework.ui.product.ProductSceneView;

import java.awt.Window;

public class LayerSourcePane extends MultiPagePane implements LayerPageContext {

    private final ProductSceneView view;
    private final Layer selectedLayer;
    private final LayerTreeModel layerTreeModel;

    public LayerSourcePane(Window parent,
                           ProductSceneView view,
                           Layer selectedLayer,
                           LayerTreeModel layerTreeModel) {
        super(parent,  "Add Layer");
        this.view = view;
        this.selectedLayer = selectedLayer;
        this.layerTreeModel = layerTreeModel;
    }

    public ProductSceneView getView() {
        return view;
    }

    public Layer getSelectedLayer() {
        return selectedLayer;
    }

    public LayerTreeModel getLayerTreeModel() {
        return layerTreeModel;
    }

    public Layer getLayer(String layerId) {
        return LayerTreeModel.getLayer(view.getRootLayer(), layerId);
    }


}
