package com.bc.ceres.glayer;

import java.awt.geom.Rectangle2D;


/**
 * Marks a class to be able to provide a viewport.
 */
public interface ViewportAware {
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
