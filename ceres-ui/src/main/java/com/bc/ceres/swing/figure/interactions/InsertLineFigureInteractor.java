package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.StyleDefaults;

import java.awt.Point;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

public class InsertLineFigureInteractor extends InsertFigureInteractor {

    private Point referencePoint;
    private boolean canceled;
    private Figure figure;
    private Line2D line;

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
        FigureEditor figureEditor = getFigureEditor(event);
        figureEditor.getFigureSelection().removeFigures();
        referencePoint = event.getPoint();
        canceled = false;
        line = new Line2D.Double(referencePoint, referencePoint);
        figure = getFigureFactory().createLinealFigure(toModelShape(event, line), StyleDefaults.INSERT_STYLE);
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
        line.setLine(referencePoint, event.getPoint());
        figure.setShape(toModelShape(event, line));
    }
}