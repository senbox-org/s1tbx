package com.bc.ceres.grendering.swing;

import com.bc.ceres.grendering.Viewport;

import java.awt.geom.Rectangle2D;


/**
 * {@link javax.swing.JComponent JComponent}s implementing this interface are views which can
 * be adjusted using the {@link com.bc.ceres.grendering.swing.ViewportScrollPane}.
 */
public interface AdjustableView {
    /**
     * @return  The viewport.
     */
    Viewport getViewport();

    /**
     * Gets the viewable area in model coordinates.
     * @return The viewable area in model coordinates.
     */
    Rectangle2D getModelBounds();
}
