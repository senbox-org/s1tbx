package com.bc.ceres.grender.swing;

import com.bc.ceres.grender.Viewport;

import java.awt.geom.Rectangle2D;


/**
 * {@link javax.swing.JComponent JComponent}s implementing this interface are views which can
 * be adjusted using the {@link com.bc.ceres.grender.swing.ViewportScrollPane}.
 */
public interface AdjustableView {
    /**
     * @return  The viewport.
     */
    Viewport getViewport();

    /**
     * @return The viewable area in model coordinates.
     */
    Rectangle2D getModelBounds();
}
