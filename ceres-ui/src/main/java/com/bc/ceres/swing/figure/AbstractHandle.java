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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The base class for all {@link Handle} implementations.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public abstract class AbstractHandle extends AbstractFigure implements Handle {

    private final Figure figure;
    private final FigureChangeListener listener;
    private final Point2D.Double location;
    private Shape shape;

    /**
     * Constructor.
     *
     * @param figure        The figure to which this handle belongs.
     * @param normalStyle   The handle's normal style.
     * @param selectedStyle The handle's selected style.
     */
    protected AbstractHandle(Figure figure,
                             FigureStyle normalStyle,
                             FigureStyle selectedStyle) {
        super(normalStyle, selectedStyle);
        setSelectable(true);
        this.figure = figure;

        this.listener = new FigureChangeListener() {
            @Override
            public void figureChanged(FigureChangeEvent e) {
                updateLocation();
            }
        };
        this.figure.addChangeListener(listener);
        this.location = new Point2D.Double();
    }

    public double getX() {
        return location.x;
    }

    public double getY() {
        return location.y;
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public Point2D getLocation() {
        return (Point2D) location.clone();
    }

    public void setLocation(Point2D location) {
        setLocation(location.getX(), location.getY());
    }

    public void setLocation(double x, double y) {
        location.setLocation(x, y);
    }

    public abstract void updateLocation();

    /**
     * @return The figure to which this handle belongs.
     */
    public Figure getFigure() {
        return figure;
    }

    @Override
    public Rank getRank() {
        return Rank.AREA;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    @Override
    public Rectangle2D getBounds() {
        return shape.getBounds2D();
    }

    /**
     * The default implementation returns {@code true}.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        Point2D delta = new Point2D.Double(point.getX() - location.getX(),
                                           point.getY() - location.getY());
        m2v.deltaTransform(delta, delta);
        return getShape().contains(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        figure.removeChangeListener(listener);
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    @Override
    public abstract void move(double dx, double dy);

    @Override
    public final void draw(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform oldTransform = g.getTransform();

        try {
            AffineTransform m2v = vp.getModelToViewTransform();
            Point2D transfLocation = m2v.transform(location, null);
            AffineTransform newTransform = new AffineTransform(oldTransform);
            newTransform.concatenate(
                    AffineTransform.getTranslateInstance(transfLocation.getX(), transfLocation.getY()));
            g.setTransform(newTransform);

            drawHandle(g);

        } finally {
            g.setTransform(oldTransform);
        }
    }

    protected void drawHandle(Graphics2D g) {
        FigureStyle handleStyle = getEffectiveStyle();

        g.setPaint(handleStyle.getFillPaint());
        g.fill(getShape());

        g.setPaint(handleStyle.getStrokePaint());
        g.setStroke(handleStyle.getStroke());
        g.draw(getShape());
    }
}