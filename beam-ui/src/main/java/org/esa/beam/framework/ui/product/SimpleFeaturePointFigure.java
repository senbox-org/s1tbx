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

package org.esa.beam.framework.ui.product;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.AbstractPointFigure;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.PointHandle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.draw.ShapeFigure;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class SimpleFeaturePointFigure extends AbstractPointFigure implements SimpleFeatureFigure {

    private final SimpleFeature simpleFeature;
    private Point geometry;
    private double radius; // number of pixels in view coordinates

    public SimpleFeaturePointFigure(SimpleFeature simpleFeature, FigureStyle style) {
        this.simpleFeature = simpleFeature;
        Object o = simpleFeature.getDefaultGeometry();
        if (!(o instanceof Point)) {
            throw new IllegalArgumentException("simpleFeature");
        }
        geometry = (Point) o;
        radius = 6.0;
        setSelectable(true);
    }

    @Override
    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public Point getGeometry() {
        return geometry;
    }

    @Override
    public void setGeometry(Geometry geometry) {
        this.geometry = (Point) geometry;
    }

    @Override
    public void forceRegeneration() {
    }

    @Override
    public double getX() {
        return geometry.getX();
    }

    @Override
    public double getY() {
        return geometry.getY();
    }

    @Override
    public void setLocation(double x, double y) {
        Coordinate coordinate = geometry.getCoordinate();
        coordinate.x = x;
        coordinate.y = y;
        geometry.geometryChanged();
        fireFigureChanged();
    }

    @Override
    public Rectangle2D getBounds() {
        final double eps = 1e-10;
        return new Rectangle2D.Double(getX() - eps, getY() - eps, 2 * eps, 2 * eps);
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {

        final double dx = point.getX() - getX();
        final double dy = point.getY() - getY();
        final Object symbolAttribute = simpleFeature.getAttribute("symbol");
        AffineTransform scaleInstance = AffineTransform.getScaleInstance(m2v.getScaleX(), m2v.getScaleY());
        Point2D delta = scaleInstance.transform(new Point2D.Double(dx, -dy), null);
        if (symbolAttribute instanceof ShapeFigure) {
            final Rectangle2D bounds = ((ShapeFigure) symbolAttribute).getBounds();
            return bounds.contains(delta);
        } else {
            return delta.getX() * delta.getX() + delta.getY() * delta.getY() < radius * radius;
        }
    }

    @Override
    protected void drawPointSymbol(Rendering rendering) {
        rendering.getGraphics().setPaint(Color.BLUE);
        rendering.getGraphics().setStroke(new BasicStroke(1.0f));
        final Object symbolAttribute = simpleFeature.getAttribute("symbol");
        if (symbolAttribute instanceof ShapeFigure) {
            ((ShapeFigure) symbolAttribute).draw(rendering.getGraphics());
        } else {
            drawCross(rendering);
        }
        if (isSelected()) {
            rendering.getGraphics().setPaint(new Color(255, 255, 0, 200));
            rendering.getGraphics().setStroke(new BasicStroke(0.5f));
            if (symbolAttribute instanceof PlacemarkSymbol) {
                ((PlacemarkSymbol) symbolAttribute).drawSelected(rendering.getGraphics());
            } else {
                drawCross(rendering);
            }
        }
        final Object labelAttribute = simpleFeature.getAttribute("label");
        if (labelAttribute instanceof String) {
            drawLabel(rendering, (String) labelAttribute);
        }
    }

    private void drawLabel(Rendering rendering, String label) {
        final Graphics2D graphics = rendering.getGraphics();
        final Font oldFont = graphics.getFont();
        final Stroke oldStroke = graphics.getStroke();
        final Paint oldPaint = graphics.getPaint();

        try {
            Font font = new Font("Helvetica", Font.BOLD, 14);
            graphics.setFont(font);
            GlyphVector glyphVector = font.createGlyphVector(graphics.getFontRenderContext(), label);
            Rectangle2D logicalBounds = glyphVector.getLogicalBounds();
            float tx = (float) (logicalBounds.getX() - 0.5 * logicalBounds.getWidth());
            float ty = (float) (1.0 + logicalBounds.getHeight());
            Shape outline = glyphVector.getOutline(tx, ty);

            int[] alphas = new int[]{64, 128, 192, 255};
            for (int i = 0; i < alphas.length; i++) {
                BasicStroke selectionStroke = new BasicStroke((alphas.length - i));
                Color selectionColor = new Color(Color.BLACK.getRed(),
                                                 Color.BLACK.getGreen(),
                                                 Color.BLACK.getBlue(),
                                                 alphas[i]);
                graphics.setStroke(selectionStroke);
                graphics.setPaint(selectionColor);
                graphics.draw(outline);
            }

            graphics.setPaint(Color.WHITE);
            graphics.fill(outline);
        } finally {
            graphics.setPaint(oldPaint);
            graphics.setStroke(oldStroke);
            graphics.setFont(oldFont);
        }
    }

    private void drawCross(Rendering rendering) {
        rendering.getGraphics().draw(new Line2D.Double(-radius, -radius, +radius, +radius));
        rendering.getGraphics().draw(new Line2D.Double(+radius, -radius, -radius, +radius));
    }

    private double getModelToViewScale(Rendering rendering) {
        double determinant = rendering.getViewport().getModelToViewTransform().getDeterminant();
        return Math.sqrt(Math.abs(determinant));
    }

    @Override
    public int getMaxSelectionStage() {
        return 2;
    }

    @Override
    public Handle[] createHandles(int selectionStage) {
        if (selectionStage == 2) {
            DefaultFigureStyle handleStyle = new DefaultFigureStyle();
            handleStyle.setStrokeColor(Color.ORANGE);
            handleStyle.setStrokeOpacity(0.8);
            handleStyle.setStrokeWidth(1.0);
            handleStyle.setFillColor(Color.WHITE);
            handleStyle.setFillOpacity(0.5);
            return new Handle[] {new PointHandle(this, handleStyle)};
            // return new Handle[] {new PointHandle(this, handleStyle, new Rectangle(-20, -20, 40, 40))};
        }
        return super.createHandles(selectionStage);
    }
}