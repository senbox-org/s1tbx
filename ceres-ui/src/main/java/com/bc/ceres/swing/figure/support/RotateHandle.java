package com.bc.ceres.swing.figure.support;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.AbstractHandle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

public class RotateHandle extends AbstractHandle {
    private final double radius;
    private Point2D point;
    private double theta;

    public RotateHandle(Figure figure, FigureStyle style) {
        super(figure, style, style);
        radius = 0.5 * figure.getBounds().getHeight() + 20;
        theta = 0;
        setHandleShape();
    }

    @Override
    protected Shape createHandleShape() {
        final Rectangle2D bounds = getFigure().getBounds();
        final double centerX = bounds.getCenterX();
        final double centerY = bounds.getCenterY();
        point = new Point2D.Double(centerX, centerY);
        Point2D point2D = new Point2D.Double(0, -radius);
        point2D = AffineTransform.getRotateInstance(theta).transform(point2D, null);
        return new Ellipse2D.Double((point.getX() + point2D.getX()) - (StyleDefaults.ROTATE_HANDLE_SIZE * 0.5),
                                    point.getY() + point2D.getY() - StyleDefaults.ROTATE_HANDLE_SIZE * 0.5,
                                    StyleDefaults.ROTATE_HANDLE_SIZE,
                                    StyleDefaults.ROTATE_HANDLE_SIZE);
    }

    @Override
    public void move(double dx, double dy) {
        RectangularShape handleShape = (RectangularShape) getShape();
        double ax = handleShape.getCenterX() - point.getX();
        double ay = handleShape.getCenterY() - point.getY();
        final double theta1 = Math.atan2(ay, ax);
        ax += dx;
        ay += dy;
        final double theta2 = Math.atan2(ay, ax);
        final double delta = theta2 - theta1;
        theta += delta;
        getFigure().rotate(point, delta);
    }

    @Override
    public void draw(Rendering rendering) {
        final Ellipse2D.Double centerCircle = new Ellipse2D.Double(point.getX() - 0.5 * StyleDefaults.ROTATE_ANCHOR_SIZE,
                                                                   point.getY() - 0.5 * StyleDefaults.ROTATE_ANCHOR_SIZE,
                                                                   StyleDefaults.ROTATE_ANCHOR_SIZE, StyleDefaults.ROTATE_ANCHOR_SIZE);
        RectangularShape handleShape = (RectangularShape) getShape();
        rendering.getGraphics().setPaint(StyleDefaults.SELECTION_DRAW_PAINT);
        rendering.getGraphics().setStroke(StyleDefaults.SELECTION_STROKE);
        rendering.getGraphics().draw(new Line2D.Double(point.getX(), point.getY(),
                                                       handleShape.getCenterX(), handleShape.getCenterY()));

        rendering.getGraphics().setPaint(getStyle().getFillPaint());
        rendering.getGraphics().fill(centerCircle);

        rendering.getGraphics().setPaint(getStyle().getDrawPaint());
        rendering.getGraphics().setStroke(getStyle().getDrawStroke());
        rendering.getGraphics().draw(centerCircle);
        rendering.getGraphics().draw(new Line2D.Double(point.getX(), point.getY(),
                                                       point.getX(), point.getY()));

        super.draw(rendering);
    }
}