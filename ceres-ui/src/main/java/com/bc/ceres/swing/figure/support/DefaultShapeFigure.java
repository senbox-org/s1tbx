package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.AbstractShapeFigure;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.Shape;
import java.awt.geom.Path2D;

public class DefaultShapeFigure extends AbstractShapeFigure {
    private Shape shape;

    public DefaultShapeFigure() {
        this(null, true, new DefaultFigureStyle());
    }

    public DefaultShapeFigure(Shape shape, boolean polygonal, FigureStyle normalStyle) {
        super(polygonal, normalStyle);
        this.shape = shape;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public void setShape(Shape path) {
        shape = path;
        fireFigureChanged();
    }

    @Override
    public DefaultShapeFigure clone() {
        DefaultShapeFigure copy = (DefaultShapeFigure) super.clone();
        copy.shape = new Path2D.Double(shape);
        return copy;
    }
}
