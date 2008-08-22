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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.esa.beam.framework.draw.Figure;
import org.esa.beam.util.PropertyMap;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

public class FigureLayer extends Layer {
        // TODO: IMAGING 4.5: Layer.getStyle(), SVG property names!
        public static final boolean DEFAULT_SHAPE_OUTLINED = true;
        public static final double DEFAULT_SHAPE_OUTL_TRANSPARENCY = 0.1;
        public static final Color DEFAULT_SHAPE_OUTL_COLOR = Color.yellow;
        public static final double DEFAULT_SHAPE_OUTL_WIDTH = 1.0;
        public static final boolean DEFAULT_SHAPE_FILLED = true;
        public static final double DEFAULT_SHAPE_FILL_TRANSPARENCY = 0.5;
        public static final Color DEFAULT_SHAPE_FILL_COLOR = Color.blue;

        private final List<Figure> figureList;
        private final AffineTransform shapeToModelTransform;
        private final AffineTransform modelToShapeTransform;
        private Map<String, Object> figureAttributes;

        public FigureLayer(Figure[] figures) {
            this.figureList = new ArrayList<Figure>(Arrays.asList(figures));
            this.shapeToModelTransform =
                    this.modelToShapeTransform = new AffineTransform();
            figureAttributes = new HashMap<String, Object>();
        }

        public void addFigure(Figure currentShapeFigure) {
            currentShapeFigure.setAttributes(figureAttributes);
            figureList.add(currentShapeFigure);
        }

        public List<Figure> getFigureList() {
            return figureList;
        }

        public void setFigureList(List<Figure> list) {
            figureList.clear();
            figureList.addAll(list);
            for (Figure figure : figureList) {
                figure.setAttributes(figureAttributes);
            }
        }

//        public AffineTransform getShapeToModelTransform() {
//            return (AffineTransform) shapeToModelTransform.clone();
//        }
//
//        public AffineTransform getModelToShapeTransform() {
//            return (AffineTransform) modelToShapeTransform.clone();
//        }

        @Override
        public Rectangle2D getBounds() {
            Rectangle2D boundingBox = new Rectangle2D.Double();
            for (Figure figure : figureList) {
                boundingBox.add(figure.getShape().getBounds2D());
            }
            return shapeToModelTransform.createTransformedShape(boundingBox).getBounds2D();
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

                for (Figure figure : figureList) {
                    figure.draw(g2d);
                }
            } finally {
                g2d.setTransform(transformSave);
            }
        }

        /**
         * @param propertyMap
         */
        public void setStyleProperties(PropertyMap propertyMap) {
            final boolean outlined = propertyMap.getPropertyBool("shape.outlined", FigureLayer.DEFAULT_SHAPE_OUTLINED);
            final float outlTransp = (float) propertyMap.getPropertyDouble("shape.outl.transparency",
                                                                           FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY);
            final Color outlColor = propertyMap.getPropertyColor("shape.outl.color", FigureLayer.DEFAULT_SHAPE_OUTL_COLOR);
            final float outlWidth = (float) propertyMap.getPropertyDouble("shape.outl.width",
                                                                          FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH);

            final boolean filled = propertyMap.getPropertyBool("shape.filled", FigureLayer.DEFAULT_SHAPE_FILLED);
            final float fillTransp = (float) propertyMap.getPropertyDouble("shape.fill.transparency",
                                                                           FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY);
            final Color fillColor = propertyMap.getPropertyColor("shape.fill.color", FigureLayer.DEFAULT_SHAPE_OUTL_COLOR);

            final AlphaComposite outlComp;
            if (outlTransp > 0.0f) {
                outlComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - outlTransp);
            } else {
                outlComp = null;
            }

            final AlphaComposite fillComp;
            if (fillTransp > 0.0f) {
                fillComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - fillTransp);
            } else {
                fillComp = null;
            }

            figureAttributes.put(Figure.OUTLINED_KEY, outlined ? Boolean.TRUE : Boolean.FALSE);
            figureAttributes.put(Figure.OUTL_COMPOSITE_KEY, outlComp);
            figureAttributes.put(Figure.OUTL_COLOR_KEY, outlColor);
            figureAttributes.put(Figure.OUTL_STROKE_KEY, new BasicStroke(outlWidth));

            figureAttributes.put(Figure.FILLED_KEY, filled ? Boolean.TRUE : Boolean.FALSE);
            figureAttributes.put(Figure.FILL_COMPOSITE_KEY, fillComp);
            figureAttributes.put(Figure.FILL_PAINT_KEY, fillColor);

            for (Figure figure : figureList) {
                figure.setAttributes(figureAttributes);
            }
            
        }

    }