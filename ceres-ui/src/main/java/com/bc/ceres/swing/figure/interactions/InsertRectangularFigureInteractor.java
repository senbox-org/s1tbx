package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.support.StyleDefaults;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

public abstract class InsertRectangularFigureInteractor extends FigureEditorInteractor {
    private Point referencePoint;
    private boolean canceled;
    private ShapeFigure figure;
    private RectangularShape rectangularShape;

    protected abstract RectangularShape createRectangularShape(Point2D point);

    @Override
    public void cancelInteraction(InputEvent event) {
        if (!canceled) {
            canceled = true;
            getFigureEditor(event).getFigureSelection().removeAllFigures();
            getFigureEditor(event).getFigureCollection().removeFigure(figure);
            super.cancelInteraction(event);
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        FigureEditor figureEditor = getFigureEditor(event);
        figureEditor.getFigureSelection().removeAllFigures();
        referencePoint = event.getPoint();
        canceled = false;
        rectangularShape = createRectangularShape(toModelPoint(event, referencePoint));
        figure = figureEditor.getFigureFactory().createPolygonalFigure(toModelShape(event, rectangularShape), StyleDefaults.INSERT_STYLE);
        figureEditor.getFigureCollection().addFigure(figure);
        startInteraction(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        FigureEditor figureEditor = getFigureEditor(event);
        if (rectangularShape.isEmpty()) {
            figureEditor.getFigureCollection().removeFigure(figure);
        } else {
            figureEditor.insertFigures(false, figure);
        }
        stopInteraction(event);
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
        figure.setShape(getViewToModelTransform(event).createTransformedShape(rectangularShape));
    }
}