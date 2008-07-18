package com.bc.ceres.glayer;


/**
 * Marks a class to be able to provide a viewport.
 */
public interface ViewportAware {
    /**
     * @return  The viewport.
     */
    Viewport getViewport();
}
