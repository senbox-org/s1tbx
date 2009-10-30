package com.bc.ceres.glayer.support;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class ShapeLayerType extends LayerType {

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
    public Layer createLayer(LayerContext ctx, PropertyContainer configuration) {
        final List<Shape> shapeList = (List<Shape>) configuration.getValue(ShapeLayerType.PROPERTY_SHAPE_LIST);
        final AffineTransform modelTransform = (AffineTransform) configuration.getValue(
                ShapeLayerType.PROPTERY_SHAPE_TO_MODEL_TRANSFORM);
        return new ShapeLayer(this, shapeList, modelTransform, configuration);
    }

    @Override
    public PropertyContainer createLayerConfig(LayerContext ctx) {
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
