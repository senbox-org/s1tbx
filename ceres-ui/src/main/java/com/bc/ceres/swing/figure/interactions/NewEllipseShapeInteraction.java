package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.interactions.NewRectangularShapeInteraction;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

public class NewEllipseShapeInteraction extends NewRectangularShapeInteraction {
    @Override
    protected RectangularShape createRectangularShape(Point2D point) {
        return new Ellipse2D.Double(point.getX(), point.getY(), 0, 0);
    }
}
