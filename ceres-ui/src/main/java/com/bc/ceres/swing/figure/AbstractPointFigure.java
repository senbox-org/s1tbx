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

package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public abstract class AbstractPointFigure extends AbstractFigure implements PointFigure {

    protected AbstractPointFigure() {
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public Point2D getLocation() {
        return new Point2D.Double(getX(), getY());
    }

    @Override
    public void setLocation(Point2D location) {
        setLocation(location.getX(), location.getY());
    }

    public abstract void setLocation(double x, double y);

    @Override
    public final Rank getRank() {
        return Rank.POINT;
    }

    @Override
    public void scale(Point2D point, double sx, double sy) {
        final double x0 = point.getX();
        final double y0 = point.getY();
        setLocation(x0 + (getX() - x0) * sx, y0 + (getY() - y0) * sy);

    }

    @Override
    public void rotate(Point2D point, double theta) {
        final AffineTransform transform = new AffineTransform();
        transform.rotate(theta, point.getX(), point.getY());
        Point2D point2D = transform.transform(getLocation(), null);
        setLocation(point2D);
    }

    @Override
    public void move(double dx, double dy) {
        setLocation(getX() + dx, getY() + dy);
    }

    @Override
    public final void draw(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform oldTransform = g.getTransform();

        try {
            AffineTransform m2v = vp.getModelToViewTransform();
            Point2D viewLocation = m2v.transform(getLocation(), null);
            g.translate(viewLocation.getX(), viewLocation.getY());

            drawPointSymbol(rendering);
        } finally {
            g.setTransform(oldTransform);
        }
    }

    protected abstract void drawPointSymbol(Rendering rendering);
}