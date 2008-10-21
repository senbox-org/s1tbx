package com.bc.ceres.grender;

import java.awt.Rectangle;
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

    // todo - add method getOffset():mp        (nf - 21.10.2008)

    // todo - change name+signature to setOffset(mp) (nf - 21.10.2008)
    /**
     * Moves the viewport to the absolute position in model coordinates.
     * @param modelPosX The position X in model coordinates.
     * @param modelPosY The position Y in model coordinates.
     */
    void move(double modelPosX, double modelPosY);

    // todo - rename to dragModelCS(vx, vy)??? (nf - 21.10.2008)
    /**
     * Moves the model CS by translating it into the opposite direction of the given
     * vector in view coordinates.
     *
     * @param viewDeltaX the X delta in view coordinates
     * @param viewDeltaY the Y delta in view coordinates
     */
    void moveViewDelta(double viewDeltaX, double viewDeltaY);

    /**
     * Gets the zoom factor.
     * The zoom factor is equal to the number of model units per view unit.
     *
     * @return The zoom factor.
     */
    double getZoomFactor();

    // todo - api doc is wrong with respect to current implementation in DefaultViewport (nf - 21.10.2008)
    // todo - rename zoom(f) to setZoomFactor(f) (nf - 21.10.2008)
    /**
     * Sets the zoom factor relative to the viewport bound's center point.
     *
     * @param zoomFactor The zoom factor.
     * @see #getZoomFactor()
     */
    void zoom(double zoomFactor);

    /**
     * Zooms to the given area given in model coordinates.
     *
     * @param modelArea the area in model coordinates
     */
    void zoom(Rectangle2D modelArea);

    /**
     * Zooms to the given center point given in model coordinates.
     *
     * @param modelCenterX The center point X in model coordinates
     * @param modelCenterY The center point Y in model coordinates
     * @param zoomFactor   The zoom factor.
     */
    void zoom(double modelCenterX, double modelCenterY, double zoomFactor);

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

    // todo - api doc: explain what does "synchronize" mean? (nf - 21.10.2008)
    // todo - discuss options  (nf - 21.10.2008)
    // (1) rename synchronizeWith(vp) to setTransform(vp)
    // (2) add method setModelToViewTransform(m2v)
    /**
     * Synchronizes this viewport with the given one.
     * 
     * @param otherViewport The view port to synchronize with
     */
    void synchronizeWith(Viewport otherViewport);
    
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
