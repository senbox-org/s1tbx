package com.bc.ceres.grendering;

import com.bc.ceres.grendering.Viewport;

import java.awt.*;


/**
 * A rendering is used to render graphical data representations to a GUI widget, image or another
 * output device such as a printer.
 */
public interface Rendering {
    /**
     * @return The bounds of the rendering in view coordinates.
     */
    Rectangle getBounds();

    /**
     * @return The graphics context associated with this rendering.
     */
    Graphics2D getGraphics();

    /**
     * @return The porthole through which the data is viewed.
     */
    Viewport getViewport();
}
