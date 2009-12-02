package com.bc.ceres.swing.figure;

import java.awt.Shape;
import java.awt.geom.Point2D;

public interface FigureFactory {
    Figure createPunctualFigure(Point2D geometry, FigureStyle style);

    Figure createLinealFigure(Shape geometry, FigureStyle style);

    Figure createPolygonalFigure(Shape geometry, FigureStyle style);

    FigureCollection createCollectionFigure(Figure... figures);
}
