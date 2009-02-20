package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.mpage.PageContext;
import org.esa.beam.framework.ui.product.ProductSceneView;


public interface LayerPageContext extends PageContext {

    ProductSceneView getView();


    Layer getSelectedLayer();

    /*
     * todo - No good here.
     */
    LayerTreeModel getLayerTreeModel();

    Layer getLayer(String layerId);
}
