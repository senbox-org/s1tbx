package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.AbstractHandle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureStyle;
import static com.bc.ceres.swing.figure.support.StyleDefaults.VERTEX_HANDLE_SIZE;

import java.awt.Shape;
import java.awt.geom.Path2D;


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
        final double[] segment = getFigure().getVertex(vertexIndex);
        double x = segment[0];
        double y = segment[1];
        setLocation(x, y);
    }

    @Override
    public void move(double dx, double dy) {
        setLocation(getX() + dx, getY() + dy);
        
        final double[] segment = getFigure().getVertex(vertexIndex);
        if (segment != null) {
            segment[0] += dx;
            segment[1] += dy;
            getFigure().setVertex(vertexIndex, segment);
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