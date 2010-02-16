package com.bc.ceres.swing.figure;

/**
 * A change listener which can be registered with a {@link Figure}.
 */
public interface FigureChangeListener {
    /**
     * Called when a figure has changed.
     *
     * @param event The event.
     */
    void figureChanged(FigureChangeEvent event);
}
