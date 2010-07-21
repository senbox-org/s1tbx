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
package org.esa.beam.glayer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.draw.Figure;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * @deprecated since BEAM 4.7, replaced by VectorDataLayer
 */
@Deprecated
public class FigureLayer extends Layer {

    public static final String PROPERTY_NAME_FIGURE_LIST = "figureList";
    public static final String PROPERTY_NAME_TRANSFORM = "shapeToModelTransform";
    public static final String PROPERTY_NAME_SHAPE_OUTLINED = "outlined";
    public static final String PROPERTY_NAME_SHAPE_FILLED = "filled";
    public static final String PROPERTY_NAME_SHAPE_OUTL_COLOR = "outlineColor";
    public static final String PROPERTY_NAME_SHAPE_FILL_COLOR = "fillColor";
    public static final String PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY = "outlineTransparency";
    public static final String PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY = "fillTransparency";
    public static final String PROPERTY_NAME_SHAPE_OUTL_WIDTH = "outlineWidth";
    public static final String PROPERTY_NAME_SHAPE_OUTL_COMPOSITE = "outlineComposite";
    public static final String PROPERTY_NAME_SHAPE_FILL_COMPOSITE = "fillComposite";

    public static final boolean DEFAULT_SHAPE_OUTLINED = Boolean.TRUE;
    public static final boolean DEFAULT_SHAPE_FILLED = Boolean.TRUE;
    public static final Color DEFAULT_SHAPE_OUTL_COLOR = Color.yellow;
    public static final Color DEFAULT_SHAPE_FILL_COLOR = Color.BLUE;
    public static final double DEFAULT_SHAPE_OUTL_TRANSPARENCY = 0.1;
    public static final double DEFAULT_SHAPE_FILL_TRANSPARENCY = 0.5;
    public static final double DEFAULT_SHAPE_OUTL_WIDTH = 1.0;

    private final List<Figure> figureList;
    private final AffineTransform shapeToModelTransform;

    public FigureLayer(FigureLayerType type, final List<Figure> figureList,
                       final AffineTransform shapeToModelTransform,
                       PropertySet configuration) {
        super(type, configuration);
        setName("Figures");
        this.figureList = figureList;
        this.shapeToModelTransform = shapeToModelTransform;
    }

    public void addFigure(Figure currentShapeFigure) {
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

            double layerOpacity = 1.0 - getTransparency();
            for (final Figure figure : figureList) {
                setAttributes(figure);
                Composite oldOutlComposite = (Composite) figure.getAttribute(Figure.OUTL_COMPOSITE_KEY);
                Composite oldFillComposite = (Composite) figure.getAttribute(Figure.FILL_COMPOSITE_KEY);
                try {
                    figure.setAttribute(Figure.OUTL_COMPOSITE_KEY, deriveComposite(oldOutlComposite, layerOpacity));
                    figure.setAttribute(Figure.FILL_COMPOSITE_KEY, deriveComposite(oldFillComposite, layerOpacity));
                    figure.draw(g2d);
                } finally {
                    figure.setAttribute(Figure.OUTL_COMPOSITE_KEY, oldOutlComposite);
                    figure.setAttribute(Figure.FILL_COMPOSITE_KEY, oldFillComposite);
                }
            }
        } finally {
            g2d.setTransform(transformSave);
        }
    }

    private Composite deriveComposite(Composite outlComposite, double opacity) {
        if (outlComposite instanceof AlphaComposite) {
            AlphaComposite outlAlphaComposite = (AlphaComposite) outlComposite;
            float newOutlAlpha = (float) opacity * outlAlphaComposite.getAlpha();
            return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, newOutlAlpha);
        }
        return outlComposite;
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
        return getConfigurationProperty(PROPERTY_NAME_SHAPE_OUTLINED, DEFAULT_SHAPE_OUTLINED);
    }

    private Color getShapeOutlineColor() {
        return getConfigurationProperty(PROPERTY_NAME_SHAPE_OUTL_COLOR, DEFAULT_SHAPE_OUTL_COLOR);
    }

    private double getShapeOutlineTransparency() {
        return getConfigurationProperty(PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, DEFAULT_SHAPE_OUTL_TRANSPARENCY);
    }

    private boolean isShapeFilled() {
        return getConfigurationProperty(PROPERTY_NAME_SHAPE_FILLED, DEFAULT_SHAPE_FILLED);
    }

    private double getShapeOutlineWidth() {
        return getConfigurationProperty(PROPERTY_NAME_SHAPE_OUTL_WIDTH, DEFAULT_SHAPE_OUTL_WIDTH);
    }

    private Color getShapeFillColor() {
        return getConfigurationProperty(PROPERTY_NAME_SHAPE_FILL_COLOR, DEFAULT_SHAPE_FILL_COLOR);
    }

    private double getShapeFillTransparency() {
        return getConfigurationProperty(PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, DEFAULT_SHAPE_FILL_TRANSPARENCY);
    }
}