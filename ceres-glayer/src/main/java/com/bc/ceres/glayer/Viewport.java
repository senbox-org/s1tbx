package com.bc.ceres.glayer;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;


/**
 * A {@code Viewport} allows to view a certain part of the graphical layers
 * defined in the model coordinates system. Therefore a viewport comprises a
 * {@link Rendering} and an affine transformation from model coordinates to
 * view coordinates and vice versa.
 *
 * @author Norman Fomferra
 */
public interface Viewport {

    /**
     * @return The affine transformation from view to model coordinates.
     */
    AffineTransform getViewToModelTransform();

    /**
     * @return The affine transformation from model to view coordinates.
     */
    AffineTransform getModelToViewTransform();

    /**
     * @return The coordinate in model CS which corresponds to origin at (0,0) in the view CS.
     */
    Point2D getModelOffset();

    /**
     * Sets the offset in model coordinates.
     *
     * @param offset The offset.
     */
    void setModelOffset(Point2D offset);

    /**
     * Sets the offset in model coordinates.
     *
     * @param offset The offset.
     */
    void setViewOffset(Point2D offset);

    /**
     * @return The size of a view pixel in model coordinates.
     */
    double getModelScale();

    /**
     * Sets the model-to-view scaling factor relative to a given center point in view coordinates.
     *
     * @param modelScale the new size of a view pixel in model coordinates
     * @param viewCenter the center of the zoom in the view CS
     */
    void setModelScale(double modelScale, Point2D viewCenter);

    /**
     * @return The rotation angle in radians.
     */
    double getModelRotation();

    /**
     * Sets the rotation angle relative to a given center point in view coordinates.
     *
     * @param theta      the new rotaton angle in radians
     * @param viewCenter the center of the zoom in the view CS
     */
    void setModelRotation(double theta, Point2D viewCenter);


    /**
     * Moves the model CS by translating it into the opposite direction of the given
     * vector in view coordinates.
     *
     * @param deltaX the X delta in view coordinates
     * @param deltaY the Y delta in view coordinates
     */
    void move(double deltaX, double deltaY);

    /**
     * Adds a change listener to this viewport.
     *
     * @param listener The listener.
     */
    void addChangeListener(ChangeListener listener);

    /**
     * Removes a change listener from this viewport.
     *
     * @param listener The listener.
     */
    void removeChangeListener(ChangeListener listener);

    /**
     * Gets all listeners added to this viewport.
     *
     * @return The listeners.
     */
    ChangeListener[] getChangeListeners();

    /**
     * A change listener.
     */
    static interface ChangeListener {
        /**
         * Called if the given viewport has changed.
         *
         * @param viewport The viewport.
         */
        void handleViewportChanged(Viewport viewport);
    }
}
