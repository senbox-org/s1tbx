package com.bc.ceres.grender;

import java.awt.Graphics2D;


/**
 * A rendering is used to render graphical data representations to a GUI widget, image or another
 * output device such as a printer.
 */
public interface Rendering {
    /**
     * @return The graphics context associated with this rendering.
     */
    Graphics2D getGraphics();

    /**
     * @return The porthole through which the model is viewed.
     */
    Viewport getViewport();
}
