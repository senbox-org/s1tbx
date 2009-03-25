package com.bc.ceres.grender;

/**
 * A change listener which can be added to viewports.
 */
public interface ViewportListener {
    // todo - declare flags: int ORIENTATION=0x01,OFFSET=0x02,SCALE=0x04;  (nf - 25.03.2009)
    // todo - replace orientationChanged by changeMask and use flags (nf - 25.03.2009)
    /**
     * Called if the given viewport has changed.
     *
     * @param viewport The viewport.
     * @param orientationChanged true, if the viewport's orientation has changed.
     */
    void handleViewportChanged(Viewport viewport, boolean orientationChanged);
}
