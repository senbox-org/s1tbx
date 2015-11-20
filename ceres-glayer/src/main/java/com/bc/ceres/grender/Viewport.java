/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.grender;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


/**
 * A {@code Viewport} allows to view a certain part of graphical data representations
 * given in coordinates defined in some model coordinate system. The {@code Viewport} assumes
 * that there is an affine similiarity transformation
 * (translation, rotation, scaling) from the view to the model coordiate system.
 * Shearing transformations are not supported, but the coordinate system's
 * Y-axes may point in different directions.
 *
 * <p>The view coordinate system is a cartesian coordinate system with the X-axis pointing
 * to the right and the Y-axis pointing downwards.
 *
 * <p>The model coordinate system is assumed to be cartesian coordinate system with the X-axis pointing
 * to the right and the Y-axis pointing either upwards or downwards.
 * See method {@link #isModelYAxisDown()}.
 *
 *
 * @author Norman Fomferra
 */
public interface Viewport extends Cloneable {
    /**
     * @return If {@code true}, the model coordinate's Y-axis points downwards. Returns {@code false} by default.
     */
    boolean isModelYAxisDown();

    /**
     * @param modelYAxisDown If {@code true}, the model coordinate's Y-axis points downwards.
     */
    void setModelYAxisDown(boolean modelYAxisDown);

    /**
     * @return The bounds in view coordinates.
     */
    Rectangle getViewBounds();

    /**
     * @param bounds The bounds in view coordinates.
     */
    void setViewBounds(Rectangle bounds);

    /**
     * @return The affine transformation from view to model coordinates.
     */
    AffineTransform getViewToModelTransform();

    /**
     * @return The affine transformation from model to view coordinates.
     */
    AffineTransform getModelToViewTransform();

    /**
     * @return The viewport's absolute X-offset in model coordinates.
     */
    double getOffsetX();

    /**
     * @return The viewport's absolute Y-offset in model coordinates.
     */
    double getOffsetY();

    /**
     * Sets the viewport's absolute offset in model coordinates.
     *
     * @param offsetX The X-offset in model coordinates.
     * @param offsetY The Y-offset in model coordinates.
     */
    void setOffset(double offsetX, double offsetY);

    /**
     * Moves the model CS by translating it into the opposite direction of the given
     * vector in view coordinates.
     *
     * @param viewDeltaX the X delta in view coordinates
     * @param viewDeltaY the Y delta in view coordinates
     */
    void moveViewDelta(double viewDeltaX, double viewDeltaY);

    // todo - use term "scale"

    /**
     * Gets the zoom factor.
     * The zoom factor is equal to the number of model units per view unit.
     *
     * @return The zoom factor.
     */
    double getZoomFactor();

    // todo - use term "scale"

    /**
     * Sets the zoom factor relative to the viewport bound's center point.
     *
     * @param zoomFactor The new zoom factor, must be greater than zero.
     * @throws IllegalArgumentException if zoomFactor is less than or equal to zero
     * @see #getZoomFactor()
     */
    void setZoomFactor(double zoomFactor);

    /**
     * Zooms to the given point given in model coordinates.
     *
     * @param zoomFactor   The new zoom factor, must be greater than zero.
     * @param modelCenterX New X of the view's center point in model coordinates.
     * @param modelCenterY New Y of the view's center point in model coordinates.
     * @throws IllegalArgumentException if zoomFactor is less than or equal to zero
     */
    void setZoomFactor(double zoomFactor, double modelCenterX, double modelCenterY);

    /**
     * Zooms to the given area given in model coordinates.
     *
     * @param modelArea the area in model coordinates
     */
    void zoom(Rectangle2D modelArea);

    /**
     * @return The rotation angle in radians.
     */
    double getOrientation();

    // todo - add method setOrientation(mx, my, ang) (nf - 21.10.2008)

    /**
     * Sets the orientation angle relative to the viewport bound's center point.
     *
     * @param orientation the new orientation angle in radians
     */
    void setOrientation(double orientation);

    /**
     * Modifies this viewport so that it matches the given one.
     *
     * @param otherViewport The view port to synchronize with.
     */
    void setTransform(Viewport otherViewport);

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

    /**
     * Creates a clone of this viewport.
     * The clone is a deep copy of this viewport but doesn't copy its listeners.
     *
     * @return The clone.
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Viewport clone();
}
