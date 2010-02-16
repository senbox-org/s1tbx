package com.bc.ceres.swing.figure;

import java.awt.geom.Point2D;


/**
 * A figure represents a graphical object.
 * Figures are graphically modified by their {@link Handle}s.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since Ceres 0.10
 */
public interface PointFigure extends Figure {

    double getX();

    double getY();

    Point2D getLocation();

    void setLocation(Point2D location);
}