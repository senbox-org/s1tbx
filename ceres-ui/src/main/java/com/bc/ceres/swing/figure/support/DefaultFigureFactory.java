package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.Shape;
import java.awt.geom.Point2D;

public class DefaultFigureFactory implements FigureFactory {
    @Override
    public Figure createPunctualFigure(Point2D geometry, FigureStyle style) {
        // todo - implement a DefaultPointFigure
        throw new IllegalStateException("Not implemented yet.");
    }

    @Override
    public Figure createLinealFigure(Shape geometry, FigureStyle style) {
        return new DefaultShapeFigure(geometry, false, style);
    }

    @Override
    public Figure createPolygonalFigure(Shape geometry, FigureStyle style) {
        return new DefaultShapeFigure(geometry, true, style);
    }

    @Override
    public FigureCollection createCollectionFigure(Figure... figures) {
        return new DefaultFigureCollection(figures);
    }
}
