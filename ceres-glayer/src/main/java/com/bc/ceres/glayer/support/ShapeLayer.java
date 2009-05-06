package com.bc.ceres.glayer.support;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
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

// todo - use style here

/**
 * A shape layer is used to draw {@link Shape}s.
 *
 * @author Norman Fomferra
 */
public class ShapeLayer extends Layer {

    private static final Type LAYER_TYPE = (Type) LayerType.getLayerType(Type.class.getName());

    private final List<Shape> shapeList;
    private final AffineTransform shapeToModelTransform;
    private final AffineTransform modelToShapeTransform;

    public ShapeLayer(Shape[] shapes) {
        this(shapes, new AffineTransform());
    }

    public ShapeLayer(Shape[] shapes, AffineTransform shapeToModelTransform) {
        this(LAYER_TYPE, initConfiguration(LAYER_TYPE.getConfigurationTemplate(), shapes, shapeToModelTransform));
    }

    public ShapeLayer(Type layerType, ValueContainer configuration) {
        super(layerType, configuration);
        this.shapeList = (List<Shape>) configuration.getValue(Type.PROPERTY_SHAPE_LIST);
        this.shapeToModelTransform = (AffineTransform) configuration.getValue(Type.PROPTERY_SHAPE_TO_MODEL_TRANSFORM);
        try {
            this.modelToShapeTransform = shapeToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("shapeToModelTransform", e);
        }
    }

    private static ValueContainer initConfiguration(ValueContainer template, Shape[] shapes,
                                                    AffineTransform shapeToModelTransform) {
        try {
            template.setValue(Type.PROPERTY_SHAPE_LIST, Arrays.asList(shapes));
            template.setValue(Type.PROPTERY_SHAPE_TO_MODEL_TRANSFORM, shapeToModelTransform.clone());
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }
        return template;
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

    public static class Type extends LayerType {

        public static final String PROPERTY_SHAPE_LIST = "shapes";
        public static final String PROPTERY_SHAPE_TO_MODEL_TRANSFORM = "shapeToModelTransform";

        @Override
        public String getName() {
            return "Shape Layer";
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
            return new ShapeLayer(this, configuration);
        }

        @Override
        public ValueContainer getConfigurationTemplate() {
            final ValueContainer vc = new ValueContainer();

            final ValueModel shapeListModel = createDefaultValueModel(PROPERTY_SHAPE_LIST, List.class);
            shapeListModel.getDescriptor().setDefaultValue(new ArrayList<Shape>());
            vc.addModel(shapeListModel);

            final ValueModel transformModel = createDefaultValueModel(PROPTERY_SHAPE_TO_MODEL_TRANSFORM,
                                                                      AffineTransform.class,
                                                                      new AffineTransform());
            vc.addModel(transformModel);

            return vc;
        }
    }
}