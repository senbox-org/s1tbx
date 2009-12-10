package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.FigureFactory;

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

    public InsertMultiPointFigureInteractor(boolean polygonal) {
        this.polygonal = polygonal;
        this.points = new ArrayList<Point2D>(8);
    }

    public boolean isPolygonal() {
        return polygonal;
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

    @Override
    public void cancelInteraction(InputEvent event) {
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
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() > 1) {
            if (points.isEmpty()) {
                getFigureEditor(event).getFigureCollection().removeFigure(figure);
                figure = null;
            } else {
                points.clear();
                FigureEditor figureEditor = getFigureEditor(event);
                figureEditor.getFigureSelection().removeAllFigures();
                if (isPolygonal()) {
                    // todo - JTS wants at least 4 coords for a polygon, what the fu...
                    //figure.removeSegment(1);
                    //figure.removeSegment(1);
                }
                figureEditor.insertFigures(false, figure);
                stopInteraction(event);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        final FigureEditor figureEditor = getFigureEditor(event);

        boolean starting = false;
        if (points.isEmpty()) {
            figureEditor.getFigureSelection().removeAllFigures();
            startInteraction(event);
            starting = true;
        }

        points.add(toModelPoint(event));
        points.add(toModelPoint(event));
        if (isPolygonal()) {
            // todo - JTS wants at least 4 coords for a polygon, what the fu...
            // points.add(toModelPoint(event));
            // points.add(toModelPoint(event));
        }

        if (starting) {
            FigureFactory factory = figureEditor.getFigureFactory();
            if (isPolygonal()) {
                figure = factory.createPolygonFigure(createPath(), figureEditor.getDefaultPolygonStyle());
            } else {
                figure = factory.createLineFigure(createPath(), figureEditor.getDefaultLineStyle());
            }
            figureEditor.getFigureCollection().addFigure(figure);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (!points.isEmpty()) {
            points.set(points.size() - 1, toModelPoint(event));
            figure.setShape(createPath());
        }
    }
}
