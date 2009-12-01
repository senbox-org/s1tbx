package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.FigureInsertEdit;
import com.bc.ceres.swing.figure.support.StyleDefaults;

import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class NewMultiPointShapeInteractor extends FigureEditorInteractor {

    private final List<Point2D> points;
    private final boolean polygonal;
    private DefaultShapeFigure figure;

    public NewMultiPointShapeInteractor(boolean polygonal) {
        this.polygonal = polygonal;
        this.points = new ArrayList<Point2D>(8);
    }

    public boolean isPolygonal() {
        return polygonal;
    }

    protected Shape createPath() {
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
            }
            super.cancelInteraction(event);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() > 1) {
            points.clear();
            getFigureEditor(event).getFigureSelection().removeFigures();
            stopInteraction(event);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        final FigureEditor figureEditor = getFigureEditor(event);
        if (points.isEmpty()) {
            figureEditor.getFigureSelection().removeFigures();
            startInteraction(event);
        }

        points.add(toModelPoint(event));
        points.add(toModelPoint(event));

        if (points.size() == 2) {
            figure = new DefaultShapeFigure(createPath(), isPolygonal(), StyleDefaults.INSERT_STYLE);
            // todo - move to FigureEditor.insert(figures)
            figureEditor.getUndoContext().postEdit(new FigureInsertEdit(figureEditor, figure));
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
