package org.esa.beam.framework.ui;

import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.product.ProductSceneView;

import java.awt.Rectangle;

import com.bc.ceres.glayer.Layer;

public interface LayerUI {

    AbstractTool getSelectTool(Layer layer, ProductSceneView view);

    void handleSelection(Layer layer, ProductSceneView view, Rectangle rectangle);
}
