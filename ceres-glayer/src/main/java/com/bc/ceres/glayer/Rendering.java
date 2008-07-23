package com.bc.ceres.glayer;

import java.awt.*;
import java.awt.geom.Rectangle2D;


/**
 * A rendering is used to render a {@link GraphicalLayer} to a GUI widget, image or another
 * output device such as a printer.
 * The part of the layer to be rendered is specified by the {@link Viewport}.
 */
public interface Rendering {
    /**
     * @return The bounds of the rendering in view coordinates.
     */
    Rectangle2D getBounds();

    /**
     * @return The graphics context associated with this rendering.
     */
    Graphics2D getGraphics();

    /**
     * @return The port through which the layer is viewed.
     */
    Viewport getViewport();
}
