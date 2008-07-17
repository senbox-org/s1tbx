package com.bc.ceres.glayer;

import java.awt.Rectangle;


/**
 * The glue between the {@link Viewport} and the device (GUI widget or printer)
 * used to render a {@link GraphicalLayer}.
 */
public interface View {
    /**
     * @return The visible region of the view in view coordinates.
     */
    Rectangle getVisibleRegion();

    /**
     * Invalidates the given region so that it becomes
     * repainted as soon as possible.
     *
     * @param region The region to be invalidated.
     */
    void invalidateRegion(Rectangle region);

    /**
     * Runs the given task in the thread that is used by the GUI library.
     * In <i>Swing</i>, this would be the <i>Event Dispatcher Thread</i> (EDT).
     *
     * @param task The task to be invoked.
     */
    void invokeLater(Runnable task);
}
