package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.interactions.AbstractInteraction;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.FigureInsertEdit;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

public abstract class NewRectangularShapeInteraction extends AbstractInteraction {
    private Point referencePoint;
    private boolean canceled;
    private DefaultShapeFigure figure;
    private RectangularShape rectangularShape;

    protected abstract RectangularShape createRectangularShape(Point2D point);

    @Override
    public void cancel() {
        if (!canceled) {
            canceled = true;
            getFigureEditor().getFigureSelection().removeFigures();
            getFigureEditor().getFigureCollection().removeFigure(figure);
            super.cancel();
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        getFigureEditor().getFigureSelection().removeFigures();
        referencePoint = event.getPoint();
        canceled = false;
        rectangularShape = createRectangularShape(referencePoint);
        figure = new DefaultShapeFigure(rectangularShape, true, new DefaultFigureStyle());
        getFigureEditor().getFigureCollection().addFigure(figure);
        start();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (rectangularShape.isEmpty()) {
            getFigureEditor().getFigureCollection().removeFigure(figure);
        } else {
            getFigureEditor().postEdit(new FigureInsertEdit(getFigureEditor(), figure));
        }
        stop();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        int width = event.getX() - referencePoint.x;
        int height = event.getY() - referencePoint.y;
        int x = referencePoint.x;
        int y = referencePoint.y;
        if (width < 0) {
            width *= -1;
            x -= width;
        }
        if (height < 0) {
            height *= -1;
            y -= height;
        }
        rectangularShape.setFrame(x, y, width, height);
        figure.setShape(rectangularShape);
    }
}