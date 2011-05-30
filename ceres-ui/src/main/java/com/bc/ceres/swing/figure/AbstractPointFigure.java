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
import com.bc.ceres.swing.figure.support.NamedSymbol;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Base class for all {@link PointFigure} implementations.
 * <p/>
 * Sub-classes have to provide the location and radius of the point in model coordinates.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public abstract class AbstractPointFigure extends AbstractFigure implements PointFigure {
    private FigureStyle normalStyle;
    private FigureStyle selectedStyle;

    /**
     * Constructor.
     *
     * @param normalStyle   The style used for the "normal" state of the figure.
     * @param selectedStyle The style used for the "selected" state of the figure.
     */
    protected AbstractPointFigure(FigureStyle normalStyle, FigureStyle selectedStyle) {
        this.normalStyle = normalStyle;
        this.selectedStyle = selectedStyle;
    }

    /**
     * @return The style used for the "normal" state of the figure.
     */
    public FigureStyle getNormalStyle() {
        return normalStyle;
    }

    /**
     * @return The style used for the "selected" state of the figure.
     */
    public FigureStyle getSelectedStyle() {
        return selectedStyle;
    }

    /**
     * @return The effective style used for the current state of the figure.
     */
    public FigureStyle getEffectiveStyle() {
        return isSelected() ? getSelectedStyle() : getNormalStyle();
    }

    /**
     * Gets the symbol used for the current state of the figure.
     *
     * @return The symbol used to display the point.
     */
    public Symbol getSymbol() {
        final Symbol symbol = getEffectiveStyle().getSymbol();
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

        final AffineTransform scaleInstance = AffineTransform.getScaleInstance(m2v.getScaleX(), m2v.getScaleY());
        final Point2D delta = scaleInstance.transform(new Point2D.Double(dx, -dy), null);
        final Symbol symbol = getSymbol();
        return symbol.containsPoint(delta.getX() + symbol.getRefX(),
                                    delta.getY() + symbol.getRefY());
    }

    @Override
    public final void draw(Rendering rendering) {
        Symbol symbol = getSymbol();
        final Graphics2D g = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform oldTransform = g.getTransform();
        try {
            AffineTransform m2v = vp.getModelToViewTransform();
            Point2D locationInView = m2v.transform(getLocation(), null);
            g.translate(locationInView.getX() + symbol.getRefX(),
                        locationInView.getY() + symbol.getRefY());
            drawPointSymbol(rendering, symbol);
        } finally {
            g.setTransform(oldTransform);
        }
    }

    /**
     * Draws the symbol used to represent the point figure.
     * Drawing of symbols is always done in <i>view</i> coordinates
     * that are translated by the symbol's reference point.
     *
     * @param rendering The rendering.
     * @param symbol    The symbol used to represent the point figure.
     */
    protected void drawPointSymbol(Rendering rendering, Symbol symbol) {
        symbol.draw(rendering, getEffectiveStyle());
    }
}