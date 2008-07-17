package com.bc.layer;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;

/**
 * A layer contributes graphical elements to a drawing represented by a {@link Graphics2D} and a {@link Viewport}.
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
     * Paints the layer using the given graphics context and viewport.
     *
     * @param g  the graphics context
     * @param vp the viewport
     */
    void paint(Graphics2D g, Viewport vp);

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

}
