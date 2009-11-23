package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.support.FigureStyle;
import com.bc.ceres.swing.figure.FigureChangeListener;
import com.bc.ceres.grender.Rendering;

import java.awt.Cursor;
import java.awt.Shape;
import java.awt.geom.Point2D;

public abstract class AbstractHandle implements Handle {
    private final Figure figure;
    private final FigureStyle style;
    private final FigureStyle selectedStyle;
    private final FigureChangeListener listener;
    private Shape shape;
    private boolean selected;

    protected AbstractHandle(Figure figure,
                             FigureStyle style,
                             FigureStyle selectedStyle) {
        this.figure = figure;
        this.style = style;
        this.selectedStyle = selectedStyle;

        this.listener = new AbstractFigureChangeListener() {
            @Override
            public void figureChanged(FigureChangeEvent e) {
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
    public void draw(Rendering rendering) {
        FigureStyle handleStyle = isSelected() ? selectedStyle : style;
        rendering.getGraphics().setPaint(handleStyle.getFillPaint());
        rendering.getGraphics().fill(getShape());
        rendering.getGraphics().setPaint(handleStyle.getDrawPaint());
        rendering.getGraphics().setStroke(handleStyle.getDrawStroke());
        rendering.getGraphics().draw(getShape());
    }
}