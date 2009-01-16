package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;

interface LayerSelectionListener {
    void layerSelectionChanged(Layer selectedLayer);
}
