package com.bc.ceres.swing.figure;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Path2D;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;

public interface FigureFactory {
    Figure createPunctualFigure(Point2D geometry, FigureStyle style);

    ShapeFigure createLinealFigure(Shape geometry, FigureStyle style);

    ShapeFigure createPolygonalFigure(Shape geometry, FigureStyle style);

    Figure createCollectionFigure(Figure... figures);
}
