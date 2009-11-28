package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.interactions.NewRectangularShapeInteractor;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

public class NewEllipseShapeInteractor extends NewRectangularShapeInteractor {
    @Override
    protected RectangularShape createRectangularShape(Point2D point) {
        return new Ellipse2D.Double(point.getX(), point.getY(), 0, 0);
    }
}
