/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.glayer;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.draw.Figure;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FigureLayer extends Layer {
    public static final String PROPERTY_NAME_SHAPE_OUTLINED = "shape.outlined";
    public static final String PROPERTY_NAME_SHAPE_FILLED = "shape.filled";
    public static final String PROPERTY_NAME_SHAPE_OUTL_COLOR = "shape.outl.color";
    public static final String PROPERTY_NAME_SHAPE_FILL_COLOR = "shape.fill.color";
    public static final String PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY = "shape.outl.transparency";
    public static final String PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY = "shape.fill.transparency";
    public static final String PROPERTY_NAME_SHAPE_OUTL_WIDTH = "shape.outl.width";
    public static final String PROPERTY_NAME_SHAPE_OUTL_COMPOSITE = "shape.outl.composite";
    public static final String PROPERTY_NAME_SHAPE_FILL_COMPOSITE = "shape.fill.composite";

    public static final boolean DEFAULT_SHAPE_OUTLINED = true;
    public static final boolean DEFAULT_SHAPE_FILLED = true;
    public static final Color DEFAULT_SHAPE_OUTL_COLOR = Color.yellow;
    public static final Color DEFAULT_SHAPE_FILL_COLOR = Color.BLUE;
    public static final double DEFAULT_SHAPE_OUTL_TRANSPARENCY = 0.1;
    public static final double DEFAULT_SHAPE_FILL_TRANSPARENCY = 0.5;
    public static final double DEFAULT_SHAPE_OUTL_WIDTH = 1.0;

    private final List<Figure> figureList;
    private final AffineTransform shapeToModelTransform;

    public FigureLayer(AffineTransform i2mTransform, Figure[] figures) {
        this.figureList = new ArrayList<Figure>(Arrays.asList(figures));
        this.shapeToModelTransform = new AffineTransform(i2mTransform);
    }

    public void addFigure(Figure currentShapeFigure) {
        setAttributes(currentShapeFigure);
        figureList.add(currentShapeFigure);
        Rectangle2D figureBounds = currentShapeFigure.getBounds();
        Rectangle2D modelBounds = shapeToModelTransform.createTransformedShape(figureBounds).getBounds2D();
        fireLayerDataChanged(modelBounds);
    }

    public void removeFigure(Figure figure) {
        figureList.remove(figure);
        Rectangle2D figureBounds = figure.getBounds();
        Rectangle2D modelBounds = shapeToModelTransform.createTransformedShape(figureBounds).getBounds2D();
        fireLayerDataChanged(modelBounds);
    }

    private void setAttributes(Figure figure) {
        figure.setAttribute(Figure.OUTLINED_KEY, isShapeOutlined());
        figure.setAttribute(Figure.OUTL_COLOR_KEY, getShapeOutlineColor());
        figure.setAttribute(Figure.OUTL_STROKE_KEY, createStroke(getShapeOutlineWidth()));
        figure.setAttribute(Figure.FILLED_KEY, isShapeFilled());
        figure.setAttribute(Figure.FILL_PAINT_KEY, getShapeFillColor());
        figure.setAttribute(Figure.OUTL_COMPOSITE_KEY, createComposite(getShapeOutlineTransparency()));
        figure.setAttribute(Figure.FILL_COMPOSITE_KEY, createComposite(getShapeFillTransparency()));
    }

    public List<Figure> getFigureList() {
        return figureList;
    }

    // todo - this method is never used. Remove? Deprecate?? (rq)
    public void setFigureList(List<Figure> list) {
        figureList.clear();
        figureList.addAll(list);
        for (final Figure figure : figureList) {
            setAttributes(figure);
        }
    }

    public AffineTransform getShapeToModelTransform() {
        return (AffineTransform) shapeToModelTransform.clone();
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        Rectangle2D boundingBox = null;
        for (final Figure figure : figureList) {
            if (boundingBox == null) {
                boundingBox = figure.getShape().getBounds2D();
            } else {
                boundingBox.add(figure.getShape().getBounds2D());
            }
        }
        return boundingBox != null ? shapeToModelTransform.createTransformedShape(boundingBox).getBounds2D() : null;
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D g2d = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform transformSave = g2d.getTransform();

        try {
            final AffineTransform transform = new AffineTransform();
            transform.concatenate(transformSave);
            transform.concatenate(vp.getModelToViewTransform());
            transform.concatenate(shapeToModelTransform);
            g2d.setTransform(transform);

            for (final Figure figure : figureList) {
                figure.draw(g2d);
            }
        } finally {
            g2d.setTransform(transformSave);
        }
    }

    private static Composite createComposite(double transparency) {
        if (transparency > 0.0) {
            return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1.0 - transparency));
        }

        return null;
    }

    private static Stroke createStroke(double width) {
        return new BasicStroke((float) width);
    }

    private boolean isShapeOutlined() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_SHAPE_OUTLINED)) {
            return (Boolean) style.getProperty(PROPERTY_NAME_SHAPE_OUTLINED);
        }

        return DEFAULT_SHAPE_OUTLINED;
    }

    private Color getShapeOutlineColor() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_SHAPE_OUTL_COLOR)) {
            return (Color) style.getProperty(PROPERTY_NAME_SHAPE_OUTL_COLOR);
        }

        return DEFAULT_SHAPE_OUTL_COLOR;
    }

    private double getShapeOutlineTransparency() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY)) {
            return (Double) style.getProperty(PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY);
        }

        return DEFAULT_SHAPE_OUTL_TRANSPARENCY;
    }

    private boolean isShapeFilled() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_SHAPE_FILLED)) {
            return (Boolean) style.getProperty(PROPERTY_NAME_SHAPE_FILLED);
        }

        return DEFAULT_SHAPE_FILLED;
    }

    private double getShapeOutlineWidth() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_SHAPE_OUTL_WIDTH)) {
            return (Double) style.getProperty(PROPERTY_NAME_SHAPE_OUTL_WIDTH);
        }

        return DEFAULT_SHAPE_OUTL_WIDTH;
    }

    private Color getShapeFillColor() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_SHAPE_FILL_COLOR)) {
            return (Color) style.getProperty(PROPERTY_NAME_SHAPE_FILL_COLOR);
        }

        return DEFAULT_SHAPE_FILL_COLOR;
    }

    private double getShapeFillTransparency() {
        final Style style = getStyle();

        if (style.hasProperty(PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY)) {
            return (Double) style.getProperty(PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY);
        }

        return DEFAULT_SHAPE_FILL_TRANSPARENCY;
    }
}