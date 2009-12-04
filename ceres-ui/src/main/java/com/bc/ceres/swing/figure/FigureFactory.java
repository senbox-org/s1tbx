package com.bc.ceres.swing.figure;

import java.awt.Shape;
import java.awt.geom.Point2D;

public interface FigureFactory {
    PointFigure createPunctualFigure(Point2D point, FigureStyle style);

    ShapeFigure createLinealFigure(Shape shape, FigureStyle style);

    ShapeFigure createPolygonalFigure(Shape shape, FigureStyle style);
}
