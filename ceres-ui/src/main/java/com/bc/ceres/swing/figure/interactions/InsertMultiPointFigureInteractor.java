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

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.ShapeFigure;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class InsertMultiPointFigureInteractor extends FigureEditorInteractor {

    private final List<Point2D> points;
    private final boolean polygonal;
    private ShapeFigure figure;
    private boolean started;

    public InsertMultiPointFigureInteractor(boolean polygonal) {
        this.polygonal = polygonal;
        this.points = new ArrayList<Point2D>(8);
    }

    public boolean isPolygonal() {
        return polygonal;
    }

    @Override
    public void cancelInteraction(InputEvent event) {
        started = false;
        if (!points.isEmpty()) {
            points.remove(points.size() - 1);
            points.remove(points.size() - 1);
            if (points.isEmpty()) {
                getFigureEditor(event).getFigureCollection().removeFigure(figure);
                figure = null;
            } else {
                figure.setShape(createPath());
            }
            super.cancelInteraction(event);
        }
    }
    
    @Override
     protected void stopInteraction(InputEvent inputEvent) {
         super.stopInteraction(inputEvent);
         started = false;
     }
 
    @Override
    public void mouseClicked(MouseEvent event) {
        if (started) {
            if (event.getClickCount() > 1) {
                if (points.isEmpty()) {
                    getFigureEditor(event).getFigureCollection().removeFigure(figure);
                    figure = null;
                } else {
                    FigureEditor figureEditor = getFigureEditor(event);
                    figureEditor.getFigureSelection().removeAllFigures();
                    if (isPolygonal()) {
                        removeNotNeededPoints();
                    }
                    figure.setShape(createPath());
                    points.clear();
                    figureEditor.insertFigures(false, figure);
                    stopInteraction(event);
                }
            }
        }
    }

    private void removeNotNeededPoints() {
        final int moreThanFour = points.size() - 4;
        int i = Math.min(2, moreThanFour);
        while (i > 0) {
            points.remove(0); // remove additional points inserted for JTS polygon
            i--;
        }
        points.remove(points.size() - 1); // remove last temporary point
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (!started) {
            started = startInteraction(event);
        }
        if (!started) {
            return;
        }

        final FigureEditor figureEditor = getFigureEditor(event);
        boolean startingNewFigure = false;
        if (points.isEmpty()) {
            figureEditor.getFigureSelection().removeAllFigures();
            startingNewFigure = true;
        }
        if (!startingNewFigure) {
            points.remove(points.size() - 1); // remove last temporary point
        }
        points.add(toModelPoint(event));
        points.add(toModelPoint(event));
        if (isPolygonal() && startingNewFigure) {
            // insert 2 additional points for JTS polygon 
            points.add(toModelPoint(event));
            points.add(toModelPoint(event));
        }

        if (startingNewFigure) {
            FigureFactory factory = figureEditor.getFigureFactory();
            if (isPolygonal()) {
                figure = factory.createPolygonFigure(createPath(), figureEditor.getDefaultPolygonStyle());
            } else {
                figure = factory.createLineFigure(createPath(), figureEditor.getDefaultLineStyle());
            }
            if (figure != null) {
                figureEditor.getFigureCollection().addFigure(figure);
            } else {
                //todo show message?
                started = false;
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (started) {
            if (!points.isEmpty()) {
                points.set(points.size() - 1, toModelPoint(event));
                figure.setShape(createPath());
            }
        }
    }

    protected Path2D createPath() {
        Point2D[] points = getPoints();
        Path2D.Double path = new Path2D.Double();
        path.moveTo(points[0].getX(), points[0].getY());
        for (int i = 1; i < points.length; i++) {
            path.lineTo(points[i].getX(), points[i].getY());
        }
        if (isPolygonal()) {
            path.closePath();
        }
        return path;
    }

    protected Point2D[] getPoints() {
        return points.toArray(new Point2D[points.size()]);
    }

}
