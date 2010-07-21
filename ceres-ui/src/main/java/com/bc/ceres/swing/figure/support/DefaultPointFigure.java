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

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.AbstractPointFigure;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class DefaultPointFigure extends AbstractPointFigure {

    private final Point2D.Double location;
    private final double radius;

    public DefaultPointFigure(Point2D location) {
        this(location, 1.0);
    }

    public DefaultPointFigure(Point2D location, double radius) {
        this.location = new Point2D.Double(location.getX(), location.getY());
        this.radius = radius;
        setSelectable(true);
    }

    @Override
    public double getX() {
        return location.x;
    }

    @Override
    public double getY() {
        return location.y;
    }

    @Override
    public void setLocation(double x, double y) {
        location.setLocation(x, y);
        fireFigureChanged();
    }

    @Override
    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(getX() - radius, getY() - radius, 2 * radius, 2 * radius);
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        double dx = point.getX() - getX();
        double dy = point.getY() - getY();
        return dx * dx + dy * dy < radius * radius;
    }

    @Override
    protected void drawPointSymbol(Rendering rendering) {
        double determinant = rendering.getViewport().getModelToViewTransform().getDeterminant();
        double scale = Math.sqrt(Math.abs(determinant));
        rendering.getGraphics().setPaint(Color.BLACK);
        rendering.getGraphics().setStroke(new BasicStroke(1.0f));
        drawCross(rendering, scale);
        if (isSelected()) {
            rendering.getGraphics().setPaint(new Color(255, 255, 0, 200));
            rendering.getGraphics().setStroke(new BasicStroke(3.0f));
            drawCross(rendering, scale);
        }
    }

    private void drawCross(Rendering rendering, double scale) {
        rendering.getGraphics().draw(
                new Line2D.Double(-scale * radius, -scale * radius,
                                  +scale * radius, +scale * radius));
        rendering.getGraphics().draw(
                new Line2D.Double(+scale * radius, -scale * radius,
                                  -scale * radius, +scale * radius));
    }

}