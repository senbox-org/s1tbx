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
     * vector in model coordinates.
     *
     * @param viewDelta the 'pan' vector in model coordinates
     */
    void move(Point2D viewDelta);
}
