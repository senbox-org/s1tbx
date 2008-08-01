package com.bc.ceres.grender;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


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
    void moveDelta(double viewDeltaX, double viewDeltaY);

    /**
     * Gets the zoom factor. The zoom factor is the number of model units per view unit.
     *
     * @return The zoom factor.
     */
    double getZoomFactor();

    /**
     * Gets the maximum zoom factor. The minimum zoom factor is defined by {@code 1.0 / getViewScaleMax()}.
     *
     * @return The maximum view scale. Negative values indicate that the zoom factor is not limited.
     */
    double getMaxZoomFactor();

    /**
     * Sets the maximum zoom factor.
     *
     * @param maxZoomFactor The maximum view scale. Negative values indicate that the zoom factor is not limited.
     * @see #getZoomFactor()
     */
    void setMaxZoomFactor(double maxZoomFactor);

    /**
     * Sets the zoom factor.
     *
     * @param zoomFactor The zoom factor.
     * @see #getZoomFactor()
     */
    void zoom(double zoomFactor);

    /**
     * Sets the zoom factor relative to the given center point in view coordinates.
     *
     * @param zoomFactor The zoom factor.
     * @param viewCenter The center point in view coordinates.
     * @see #getZoomFactor()
     */
    void zoom(double zoomFactor, Point2D viewCenter);

    /**
     * @return The rotation angle in radians.
     */
    double getOrientation();

    /**
     * Sets the rotation angle relative to a given center point in view coordinates.
     *
     * @param orientation      the new rotaton angle in radians
     * @param viewCenter the center of the zoom in the view CS
     */
    void rotate(double orientation, Point2D viewCenter);


    /**
     * Zooms to the given area in model coordinates.
     *
     * @param modelArea the area in model coordinates
     */
    void zoom(Rectangle2D modelArea);

    /**
     * Zooms to the given center point in model coordinates.
     *
     * @param modelCenterX The center point X in model coordinates
     * @param modelCenterY The center point Y in model coordinates
     * @param zoomFactor The zoom factor.
     */
    void zoom(double modelCenterX, double modelCenterY, double zoomFactor) ;


    // todo - remove this later
 /**
     * This method sets all view properties of this model with a single method call.
     * The method results in a single change event being generated. This is
     * convenient when you need to adjust all the model data simultaneously and
     * do not want individual change events to occur.
     *
     * @param modelOffsetX the x-offset in model coordinates of the upper left view pixel
     * @param modelOffsetY the y-offset in model coordinates of the upper left view pixel
     * @param zoomFactor the new zoom factor
     */
    void setModelOffset(double modelOffsetX, double modelOffsetY, double zoomFactor);


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
