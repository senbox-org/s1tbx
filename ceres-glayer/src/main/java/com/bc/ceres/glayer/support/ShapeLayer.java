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

package com.bc.ceres.glayer.support;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A shape layer is used to draw {@link Shape}s.
 *
 * @author Norman Fomferra
 */
public class ShapeLayer extends Layer {

    private final List<Shape> shapeList;
    private final AffineTransform shapeToModelTransform;
    private final AffineTransform modelToShapeTransform;

    public ShapeLayer(Shape[] shapes, AffineTransform shapeToModelTransform) {
        this(LayerTypeRegistry.getLayerType(Type.class), Arrays.asList(shapes), shapeToModelTransform);
    }

    private ShapeLayer(Type layerType,List<Shape> shapes, AffineTransform shapeToModelTransform) {
       this(layerType, shapes, shapeToModelTransform, layerType.createLayerConfig(null));
    }

    public ShapeLayer(Type layerType, List<Shape> shapes,
                      AffineTransform shapeToModelTransform, PropertySet configuration) {
        super(layerType, configuration);
        this.shapeList = new ArrayList<Shape>(shapes);
        this.shapeToModelTransform = shapeToModelTransform;
        try {
            this.modelToShapeTransform = shapeToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("shapeToModelTransform", e);
        }
    }

    public List<Shape> getShapeList() {
        return new ArrayList<Shape>(shapeList);
    }

    public void setShapeList(List<Shape> list) {
        shapeList.clear();
        shapeList.addAll(list);
    }

    public AffineTransform getShapeToModelTransform() {
        return (AffineTransform) shapeToModelTransform.clone();
    }

    public AffineTransform getModelToShapeTransform() {
        return (AffineTransform) modelToShapeTransform.clone();
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        Rectangle2D shapeBounds = null;
        for (Shape shape : shapeList) {
            if (shapeBounds == null) {
                shapeBounds = shape.getBounds2D();
            } else {
                shapeBounds.add(shape.getBounds2D());
            }
        }
        return shapeBounds != null ? shapeToModelTransform.createTransformedShape(shapeBounds).getBounds2D() : null;
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        final Viewport vp = rendering.getViewport();
        final AffineTransform transformSave = g.getTransform();
        try {
            final AffineTransform transform = new AffineTransform();
            transform.concatenate(vp.getModelToViewTransform());
            transform.concatenate(shapeToModelTransform);
            g.setTransform(transform);
            for (Shape shape : shapeList) {
                g.setPaint(Color.WHITE);
                g.fill(shape);
                g.setPaint(Color.BLACK);
                g.draw(shape);
            }
        } finally {
            g.setTransform(transformSave);
        }
    }

    @LayerTypeMetadata(name = "ShapeLayerType",
                       aliasNames = {"com.bc.ceres.glayer.support.ShapeLayer$Type"})
    public static class Type extends LayerType {

        public static final String PROPERTY_SHAPE_LIST = "shapes";
        public static final String PROPTERY_SHAPE_TO_MODEL_TRANSFORM = "shapeToModelTransform";

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public Layer createLayer(LayerContext ctx, PropertySet configuration) {
            final List<Shape> shapeList = (List<Shape>) configuration.getValue(PROPERTY_SHAPE_LIST);
            final AffineTransform modelTransform = (AffineTransform) configuration.getValue(
                    PROPTERY_SHAPE_TO_MODEL_TRANSFORM);
            return new ShapeLayer(this, shapeList, modelTransform, configuration);
        }

        @Override
        public PropertySet createLayerConfig(LayerContext ctx) {
            final PropertyContainer vc = new PropertyContainer();

            final Property shapeList = Property.create(PROPERTY_SHAPE_LIST, List.class);
            shapeList.getDescriptor().setDefaultValue(new ArrayList<Shape>());
            vc.addProperty(shapeList);

            final Property transform = Property.create(PROPTERY_SHAPE_TO_MODEL_TRANSFORM, AffineTransform.class,
                                                       new AffineTransform(), true);
            vc.addProperty(transform);

            return vc;
        }
    }
}