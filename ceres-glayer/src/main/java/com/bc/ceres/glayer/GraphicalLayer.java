package com.bc.ceres.glayer;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;

/**
 * A layer contributes graphical elements to a drawing represented by a {@link Rendering}.
 *
 * @author Norman Fomferra
 */
public interface GraphicalLayer {
    /**
     * Gets the bounding box of the layer in model coordinates.
     *
     * @return the bounding box of the layer in model coordinates
     */
    Rectangle2D getBoundingBox();

    /**
     * Renders the layer.
     *
     * @param rendering The rendering to which the layer will be rendered.
     */
    void render(Rendering rendering);

    /**
     * Indicates that the layer will no longer be in use.
     * Implementors should get rid of all allocated resources.
     */
    void dispose();


//////////////////////////////// Common properties

    boolean isVisible();
    void setVisible(boolean visible);

    String getName();
    void setName(String name);

    float getTransparency();
    void setTransparency(float value);

    AlphaCompositeMode getAlphaCompositeMode();
    void setAlphaCompositeMode(AlphaCompositeMode mode);

    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);
    void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener);

    void removePropertyChangeListener(PropertyChangeListener propertyChangeListener);
    void removePropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener);

// todo - add ChangeListener support
//    /**
//     * Adds a change listener to this layer.
//     *
//     * @param listener The listener.
//     */
//    void addChangeListener(ChangeListener listener);
//
//    /**
//     * Removes a change listener from this layer.
//     *
//     * @param listener The listener.
//     */
//    void removeChangeListener(ChangeListener listener);
//
//    /**
//     * Gets all listeners added to this layer.
//     *
//     * @return The listeners.
//     */
//    ChangeListener[] getChangeListeners();
//
//    /**
//     * A change listener.
//     */
//    static interface ChangeListener {
//        /**
//         * Called if the given layer has changed.
//         *
//         * @param layer The layer.
//         */
//        void handleLayerChanged(GraphicalLayer layer);
//    }
}
