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
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.NamedSymbol;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Base class for all {@link PointFigure} implementations.
 * <p>
 * Sub-classes have to provide the location and radius of the point in model coordinates.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public abstract class AbstractPointFigure extends AbstractFigure implements PointFigure {

    /**
     * Constructor. The rank will always be {@link Rank#POINT}.
     *
     * @param normalStyle   The style used for the "normal" state of the figure.
     * @param selectedStyle The style used for the "selected" state of the figure.
     */
    protected AbstractPointFigure(FigureStyle normalStyle, FigureStyle selectedStyle) {
        super(normalStyle, selectedStyle);
    }

    /**
     * Gets the symbol used for the current state of the figure.
     *
     * @return The symbol used to display the point.
     */
    @Override
    public Symbol getSymbol() {
        final Symbol symbol = getNormalStyle().getSymbol();
        if (symbol != null) {
            return symbol;
        }
        return NamedSymbol.CROSS;
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

    /**
     * @return The point radius in model coordinates.
     */
    public abstract double getRadius();

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
    public Rectangle2D getBounds() {
        final double r = getRadius();
        return new Rectangle2D.Double(getX() - r, getY() - r, 2 * r, 2 * r);
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        final double dx = point.getX() - getX();
        final double dy = point.getY() - getY();

        final double r = getRadius();
        if (dx * dx + dy * dy < r * r) {
            return true;
        }

        final Symbol symbol = getSymbol();
        if (symbol == null) {
            return false;
        }

        final Point2D locationInView = m2v.transform(getLocation(), null);
        final Point2D pointInView = m2v.transform(point, null);
        return symbol.isHitBy(pointInView.getX() - locationInView.getX(),
                              pointInView.getY() - locationInView.getY());
    }

    @Override
    public FigureStyle getEffectiveStyle() {
        if (isSelected()) {
            final DefaultFigureStyle style = new DefaultFigureStyle(getNormalStyle());
            //style.setStrokeColor(getSelectedStyle().getStrokeColor());
            //style.setStrokeOpacity(getSelectedStyle().getStrokeOpacity());
            return style;
        }
        return getNormalStyle();
    }

    @Override
    public final void draw(Rendering rendering) {
        final Viewport vp = rendering.getViewport();
        final AffineTransform m2v = vp.getModelToViewTransform();
        final Point2D locationInView = m2v.transform(getLocation(), null);
        if (!Double.isNaN(locationInView.getX()) && !Double.isNaN(locationInView.getY())) {
            final Graphics2D g = rendering.getGraphics();
            try {
                g.translate(locationInView.getX(), locationInView.getY());
                drawPoint(rendering);
            } finally {
                g.translate(-locationInView.getX(), -locationInView.getY());
            }
        }
    }

    /**
     * Draws the {@link #getSymbol() symbol} and other items that are used to graphically represent
     * the figure, for example labels.
     * For convenience, the rendering's drawing context is translated
     * by the point's location, so that drawing of items can be performed in symbol
     * coordinates using view units.
     *
     * @param rendering The rendering.
     */
    protected void drawPoint(Rendering rendering) {
        getSymbol().draw(rendering, getEffectiveStyle());
    }

}