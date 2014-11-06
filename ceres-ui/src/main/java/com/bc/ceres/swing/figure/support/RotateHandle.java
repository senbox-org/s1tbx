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

package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.AbstractHandle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

/**
 * A {@link com.bc.ceres.swing.figure.Handle Handle} that can be used to rotate figures.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class RotateHandle extends AbstractHandle {
    private final double radius;
    private double theta;

    public RotateHandle(Figure figure, FigureStyle style) {
        super(figure, style, style);
        radius = 80;
        theta = 0;
        setLocation();
        setShape(createHandleShape(radius, theta));
    }

    @Override
    public void updateLocation() {
        setLocation();
    }

    private void setLocation() {
        final Rectangle2D bounds = getFigure().getBounds();
        final double centerX = bounds.getCenterX();
        final double centerY = bounds.getCenterY();
        setLocation(centerX, centerY);
    }

    @Override
    public void move(double dx, double dy) {
        setLocation(getX() + dx, getY() + dy);

        RectangularShape handleShape = (RectangularShape) getShape();
        double ax = handleShape.getCenterX() - getX();
        double ay = handleShape.getCenterY() - getY();
        final double theta1 = Math.atan2(ay, ax);
        ax += dx;
        ay += dy;
        final double theta2 = Math.atan2(ay, ax);
        final double delta = theta2 - theta1;
        theta += delta;
        getFigure().rotate(getLocation(), delta);
    }

    @Override
    public void drawHandle(Graphics2D g) {
        RectangularShape handleShape = (RectangularShape) getShape();
        Shape anchorShape = new Ellipse2D.Double(-0.5 * StyleDefaults.ROTATE_ANCHOR_SIZE,
                                                 -0.5 * StyleDefaults.ROTATE_ANCHOR_SIZE,
                                                 StyleDefaults.ROTATE_ANCHOR_SIZE,
                                                 StyleDefaults.ROTATE_ANCHOR_SIZE);
        Line2D connectionLine = new Line2D.Double(0.0,
                                        0.0,
                                        handleShape.getCenterX(),
                                        handleShape.getCenterY());

        g.setPaint(StyleDefaults.SELECTION_STROKE_PAINT);
        g.setStroke(StyleDefaults.SELECTION_STROKE);
        g.draw(connectionLine);

        g.setPaint(getNormalStyle().getFillPaint());
        g.fill(anchorShape);

        g.setPaint(getNormalStyle().getStrokePaint());
        g.setStroke(getNormalStyle().getStroke());
        g.draw(anchorShape);
        g.draw(new Line2D.Double(0, 0, 0, 0));

        super.drawHandle(g);
    }

    private static Shape createHandleShape(double radius, double theta) {
        Point2D rotateHandleCenter = new Point2D.Double(0, -radius);
        rotateHandleCenter = AffineTransform.getRotateInstance(theta).transform(rotateHandleCenter, null);
        return new Ellipse2D.Double(rotateHandleCenter.getX() - StyleDefaults.ROTATE_HANDLE_SIZE * 0.5,
                                    rotateHandleCenter.getY() - StyleDefaults.ROTATE_HANDLE_SIZE * 0.5,
                                    StyleDefaults.ROTATE_HANDLE_SIZE,
                                    StyleDefaults.ROTATE_HANDLE_SIZE);
    }
}