package com.bc.ceres.grender;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;


/**
 * A {@code Viewport} allows to view a certain part of graphical data representations
 * defined in the model coordinate. Therefore a viewport comprises the view bounds and
 * the affine transformations from model coordinates to
 * view coordinates and vice versa.
 *
 * @author Norman Fomferra
 */
public interface Viewport {
    /**
     * @return The bounds in view coordinates.
     */
    Rectangle getBounds();

    /**
     * @param bounds The bounds in view coordinates.
     */
    void setBounds(Rectangle bounds);

    /**
     * @return The affine transformation from view to model coordinates.
     */
    AffineTransform getViewToModelTransform();

    /**
     * @return The affine transformation from model to view coordinates.
     */
    AffineTransform getModelToViewTransform();

    /**
     * Moves the model CS by translating it into the opposite direction of the given
     * vector in view coordinates.
     *
     * @param viewDeltaX the X delta in view coordinates
     * @param viewDeltaY the Y delta in view coordinates
     */
    void move(double viewDeltaX, double viewDeltaY);

    /**
     * Gets the zoom factor. The zoom factor is the number of model units per view unit.
     *
     * @return The zoom factor.
     */
    double getZoomFactor();

    /**
     * Sets the zoom factor.
     *
     * @param zoomFactor The zoom factor.
     * @see #getZoomFactor()
     */
    void setZoomFactor(double zoomFactor);

    /**
     * Sets the zoom factor relative to the given center point in view coordinates.
     *
     * @param zoomFactor The zoom factor.
     * @param viewCenter The center point in view coordinates.
     * @see #getZoomFactor()
     */
    void setZoomFactor(double zoomFactor, Point2D viewCenter);

    /**
     * @return The rotation angle in radians.
     */
    double getRotationAngle();

    /**
     * Sets the rotation angle relative to a given center point in view coordinates.
     *
     * @param theta      the new rotaton angle in radians
     * @param viewCenter the center of the zoom in the view CS
     */
    void setRotationAngle(double theta, Point2D viewCenter);


    /**
     * Adds a change listener to this viewport.
     *
     * @param listener The listener.
     */
    void addListener(ViewportListener listener);

    /**
     * Removes a change listener from this viewport.
     *
     * @param listener The listener.
     */
    void removeListener(ViewportListener listener);

    /**
     * Gets all listeners added to this viewport.
     *
     * @return The listeners.
     */
    ViewportListener[] getListeners();

}
