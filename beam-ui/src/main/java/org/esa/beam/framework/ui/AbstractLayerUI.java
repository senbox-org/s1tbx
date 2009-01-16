package org.esa.beam.framework.ui;

import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.product.ProductSceneView;
import com.bc.ceres.glayer.Layer;

import java.awt.Rectangle;

public abstract class AbstractLayerUI implements LayerUI {
    public AbstractTool getSelectTool(Layer layer, ProductSceneView view) {
        return null;
    }

    public void handleSelection(Layer layer, ProductSceneView view, Rectangle rectangle) {
    }
}
