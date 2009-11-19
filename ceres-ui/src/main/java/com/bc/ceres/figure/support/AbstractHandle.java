package com.bc.ceres.figure.support;

import com.bc.ceres.figure.Handle;
import com.bc.ceres.figure.Figure;
import com.bc.ceres.figure.support.FigureStyle;
import com.bc.ceres.figure.FigureListener;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;

public abstract class AbstractHandle implements Handle {
    private final Figure figure;
    private final FigureStyle style;
    private final FigureStyle selectedStyle;
    private final FigureListener listener;
    private Shape shape;
    private boolean selected;

    protected AbstractHandle(Figure figure,
                             FigureStyle style,
                             FigureStyle selectedStyle) {
        this.figure = figure;
        this.style = style;
        this.selectedStyle = selectedStyle;

        this.listener = new AbstractFigureListener() {
            @Override
            public void figureChanged(Figure f) {
                setHandleShape();
            }
        };
        this.figure.addListener(listener);
    }

    @Override
    public boolean isSelectable() {
        return false;
    }

    public Figure getFigure() {
        return figure;
    }

    public FigureStyle getStyle() {
        return style;
    }

    public Shape getShape() {
        return shape;
    }

    protected void setShape(Shape shape) {
        this.shape = shape;
    }

    protected void setHandleShape() {
        setShape(createHandleShape());
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    protected abstract Shape createHandleShape();

    @Override
    public boolean contains(Point2D point) {
        return getShape().contains(point);
    }

    @Override
    public void dispose() {
        figure.removeListener(listener);
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    @Override
    public void draw(Graphics2D g2d) {
        FigureStyle handleStyle = isSelected() ? selectedStyle : style;
        g2d.setPaint(handleStyle.getFillPaint());
        g2d.fill(getShape());
        g2d.setPaint(handleStyle.getDrawPaint());
        g2d.setStroke(handleStyle.getDrawStroke());
        g2d.draw(getShape());
    }
}