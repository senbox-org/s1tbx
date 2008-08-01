package com.bc.ceres.grender;

/**
 * A change listener which can be added to viewports.
 */
public interface ViewportListener {
    /**
     * Called if the given viewport has changed.
     *
     * @param viewport The viewport.
     * @param orientationChanged
     */
    void handleViewportChanged(Viewport viewport, boolean orientationChanged);
}
