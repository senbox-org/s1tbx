package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.AbstractHandle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureStyle;
import static com.bc.ceres.swing.figure.support.StyleDefaults.VERTEX_HANDLE_SIZE;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;


public class VertexHandle extends AbstractHandle {
    private final int vertexIndex;

    public VertexHandle(Figure figure,
                        int vertexIndex,
                        FigureStyle style,
                        FigureStyle selectedStyle) {
        super(figure, style, selectedStyle);
        this.vertexIndex = vertexIndex;
        updateLocation();
        setShape(createHandleShape());
    }

    @Override
    public void updateLocation() {
        final Point2D vertex = getFigure().getVertex(vertexIndex);
        if (vertex != null) {
            setLocation(vertex.getX(), vertex.getY());
        }
    }

    @Override
    public void move(double dx, double dy) {
        setLocation(getX() + dx, getY() + dy);
        final Point2D vertex = getFigure().getVertex(vertexIndex);
        if (vertex != null) {
            getFigure().setVertex(vertexIndex, new Point2D.Double(vertex.getX()+dx, vertex.getY()+dy));
        }
    }

    private static Shape createHandleShape() {
        Path2D path = new Path2D.Double();
        path.moveTo(0.0, -0.5 * VERTEX_HANDLE_SIZE);
        path.lineTo(0.5 * VERTEX_HANDLE_SIZE, 0.0);
        path.lineTo(0.0, 0.5 * VERTEX_HANDLE_SIZE);
        path.lineTo(-0.5 * VERTEX_HANDLE_SIZE, 0.0);
        path.closePath();
        return path;
    }
}