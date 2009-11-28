package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.AbstractInteractor;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.FigureInsertEdit;
import com.bc.ceres.swing.figure.support.StyleDefaults;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

public abstract class NewRectangularShapeInteractor extends AbstractInteractor {
    private Point referencePoint;
    private boolean canceled;
    private DefaultShapeFigure figure;
    private RectangularShape rectangularShape;

    protected abstract RectangularShape createRectangularShape(Point2D point);

    @Override
    public void cancelInteraction() {
        if (!canceled) {
            canceled = true;
            getFigureEditor().getFigureSelection().removeFigures();
            getFigureEditor().getFigureCollection().removeFigure(figure);
            super.cancelInteraction();
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        getFigureEditor().getFigureSelection().removeFigures();
        referencePoint = event.getPoint();
        canceled = false;
        rectangularShape = createRectangularShape(toModelPoint(referencePoint));
        figure = new DefaultShapeFigure(v2m().createTransformedShape(rectangularShape), true, StyleDefaults.INSERT_STYLE);
        getFigureEditor().getFigureCollection().addFigure(figure);
        startInteraction();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (rectangularShape.isEmpty()) {
            getFigureEditor().getFigureCollection().removeFigure(figure);
        } else {
            // todo - move to FigureEditor.insert(figure, memento)
            getFigureEditor().getUndoContext().postEdit(new FigureInsertEdit(getFigureEditor(), figure));
        }
        stopInteraction();
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
        figure.setShape(v2m().createTransformedShape(rectangularShape));
    }
}