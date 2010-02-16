package com.bc.ceres.swing.figure;

import java.awt.Shape;
import java.awt.geom.Point2D;

public interface FigureFactory {
    PointFigure createPointFigure(Point2D point, FigureStyle style);

    ShapeFigure createLineFigure(Shape shape, FigureStyle style);

    ShapeFigure createPolygonFigure(Shape shape, FigureStyle style);
}
