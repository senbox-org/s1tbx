package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.support.StyleDefaults;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class InsertLineFigureInteractor extends InsertFigureInteractor {

    private boolean canceled;
    private Figure figure;

    public InsertLineFigureInteractor() {
    }

    @Override
    public void cancelInteraction(InputEvent event) {
        if (!canceled) {
            canceled = true;
            getFigureEditor(event).getFigureSelection().removeFigures();
            getFigureEditor(event).getFigureCollection().removeFigure(figure);
            super.cancelInteraction(event);
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        canceled = false;

        FigureEditor figureEditor = getFigureEditor(event);
        figureEditor.getFigureSelection().removeFigures();
        Point2D referencePoint = toModelPoint(event);

        Path2D linePath = new Path2D.Double();
        linePath.moveTo(referencePoint.getX(), referencePoint.getY());
        linePath.lineTo(referencePoint.getX(), referencePoint.getY());

        figure = getFigureFactory().createLinealFigure(linePath, StyleDefaults.INSERT_STYLE);
        figureEditor.getFigureCollection().addFigure(figure);
        startInteraction(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        FigureEditor figureEditor = getFigureEditor(event);
        figureEditor.insertFigures(false, figure);
        stopInteraction(event);
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        double[] segment = figure.getSegment(1);
        Point2D referencePoint = toModelPoint(event);
        segment[0] = referencePoint.getX();
        segment[1] = referencePoint.getY();
        figure.setSegment(1, segment);
    }
}