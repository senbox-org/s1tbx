package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.AbstractInteractor;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.FigureInsertEdit;

import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class NewMultiPointShapeInteractor extends AbstractInteractor {
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
    public void cancelInteraction() {
        if (points.size() > 0) {
            points.remove(points.size() - 1);
            points.remove(points.size() - 1);
            if (points.isEmpty()) {
                getFigureEditor().getFigureCollection().removeFigure(figure);
                figure = null;
            }
            super.cancelInteraction();
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() > 1) {
            points.clear();
            getFigureEditor().getFigureSelection().removeFigures();
            stopInteraction();
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (points.isEmpty()) {
            getFigureEditor().getFigureSelection().removeFigures();
            startInteraction();
        }

        points.add(toModelPoint(event.getPoint()));
        points.add(toModelPoint(event.getPoint()));

        if (points.size() == 2) {
            figure = new DefaultShapeFigure(createPath(), isPolygonal(), new DefaultFigureStyle());
            // todo - move to FigureEditor.insert(figures)
            getFigureEditor().getUndoContext().postEdit(new FigureInsertEdit(getFigureEditor(), figure));
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (points.size() > 0) {
            points.set(points.size() - 1, toModelPoint(event.getPoint()));
            figure.setShape(createPath());
        }
    }
}
