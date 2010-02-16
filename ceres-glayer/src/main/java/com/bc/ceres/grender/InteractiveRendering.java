package com.bc.ceres.grender;

import java.awt.*;


/**
 * An interactive rendering is used to render graphical data representations on a GUI widget, allowing for
 * rendering of invalidated regions.
 */
public interface InteractiveRendering extends Rendering {

    /**
     * Invalidates the given view region so that it becomes
     * repainted as soon as possible.
     *
     * @param region The region to be invalidated (in view coordinates).
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