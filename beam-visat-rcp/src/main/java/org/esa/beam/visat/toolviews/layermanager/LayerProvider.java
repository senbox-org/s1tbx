package org.esa.beam.visat.toolviews.layermanager;

import com.jidesoft.swing.CheckBoxTree;
import com.bc.ceres.glayer.Layer;

import java.awt.Window;

// Experimental code!!!
interface LayerProvider {

    // todo - replace CheckBoxTree by some interface
    void addLayers(Window window, LayerTreeModel layerTreeModel, Layer selectedLayer);

    // todo - replace CheckBoxTree by some interface
    void removeLayers(Window window, LayerTreeModel layerTreeModel, Layer selectedLayer);
}
