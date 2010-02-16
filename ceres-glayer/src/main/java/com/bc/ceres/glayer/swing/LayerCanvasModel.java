package com.bc.ceres.glayer.swing;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportListener;

public interface LayerCanvasModel {
    Layer getLayer();

    Viewport getViewport();

    void addChangeListener(ChangeListener listener);

    void removeChangeListener(ChangeListener listener);

    public static interface ChangeListener extends LayerListener, ViewportListener {
    }
}
