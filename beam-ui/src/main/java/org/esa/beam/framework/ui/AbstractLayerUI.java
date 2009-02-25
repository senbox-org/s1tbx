package org.esa.beam.framework.ui;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.AbstractTool;

import java.awt.Rectangle;

public abstract class AbstractLayerUI implements LayerUI {
    private final Layer layer;

    protected AbstractLayerUI(Layer layer) {
        Assert.notNull(layer, "layer");
        this.layer = layer;
    }

    @Override
    public Layer getLayer() {
        return layer;
    }

    @Override
    public AbstractTool getSelectTool(ProductSceneView view) {
        return null;
    }

    @Override
    public void handleSelection(ProductSceneView view, Rectangle rectangle) {
    }
}
