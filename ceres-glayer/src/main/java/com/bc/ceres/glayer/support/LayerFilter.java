package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Layer;

public interface LayerFilter {
    boolean accept(Layer layer);
}
