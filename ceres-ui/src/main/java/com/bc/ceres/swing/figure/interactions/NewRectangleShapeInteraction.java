package com.bc.ceres.swing.figure.interactions;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

public class NewRectangleShapeInteraction extends NewRectangularShapeInteraction {
    @Override
    protected RectangularShape createRectangularShape(Point2D point) {
        return new Rectangle2D.Double(point.getX(), point.getY(), 0, 0);
    }

}
