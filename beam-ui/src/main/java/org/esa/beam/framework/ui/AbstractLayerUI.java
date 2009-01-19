package org.esa.beam.framework.ui;

import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.product.ProductSceneView;

import java.awt.Rectangle;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.core.Assert;

public abstract class AbstractLayerUI implements LayerUI {
    private final Layer layer;

    protected AbstractLayerUI(Layer layer) {
        Assert.notNull(layer, "layer");
        this.layer = layer;
    }

    public Layer getLayer() {
        return layer;
    }

    public AbstractTool getSelectTool(ProductSceneView view) {
        return null;
    }

    public void handleSelection(ProductSceneView view, Rectangle rectangle) {
    }
}
