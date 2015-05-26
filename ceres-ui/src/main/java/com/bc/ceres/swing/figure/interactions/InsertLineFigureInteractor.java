/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.ShapeFigure;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class InsertLineFigureInteractor extends FigureEditorInteractor {

    private boolean canceled;
    private Figure figure;
    private boolean started;

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
    protected void stopInteraction(InputEvent inputEvent) {
        super.stopInteraction(inputEvent);
        started = false;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        canceled = false;
        started = startInteraction(event);
        if (started) {
            FigureEditor figureEditor = getFigureEditor(event);
            figureEditor.getFigureSelection().removeAllFigures();
            Point2D referencePoint = toModelPoint(event);

            Path2D linePath = new Path2D.Double();
            linePath.moveTo(referencePoint.getX(), referencePoint.getY());
            linePath.lineTo(referencePoint.getX(), referencePoint.getY());

            final ShapeFigure lineFigure = figureEditor.getFigureFactory().createLineFigure(linePath, figureEditor.getDefaultLineStyle());
            if (lineFigure != null) {
                figure = lineFigure;
                figureEditor.getFigureCollection().addFigure(figure);
            } else {
                //todo show message
                started = false;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (started) {
            FigureEditor figureEditor = getFigureEditor(event);
            figureEditor.insertFigures(false, figure);
            stopInteraction(event);
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (started) {
            double[] segment = figure.getSegment(1);
            Point2D referencePoint = toModelPoint(event);
            segment[0] = referencePoint.getX();
            segment[1] = referencePoint.getY();
            figure.setSegment(1, segment);
        }
    }
}