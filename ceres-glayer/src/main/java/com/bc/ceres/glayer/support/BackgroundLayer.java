package com.bc.ceres.glayer.support;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;

/**
 * A background layer is used to draw a background using a unique {@link java.awt.Color}.
 *
 * @author Norman Fomferra
 */
public class BackgroundLayer extends Layer {

    private static final Type LAYER_TYPE = (Type) LayerType.getLayerType(Type.class.getName());

    public BackgroundLayer(Color color) {
        this(LAYER_TYPE, initConfiguration(LAYER_TYPE.getConfigurationTemplate(), color));
    }

    public BackgroundLayer(Type type, ValueContainer configuration) {
        super(type, configuration);
    }

    private static ValueContainer initConfiguration(ValueContainer configuration, Color color) {
        try {
            configuration.setValue(Type.COLOR, color);
        } catch (ValidationException e) {
            e.printStackTrace();
        }
        return configuration;
    }

    Color getColor() {
        return (Color) getConfiguration().getValue(Type.COLOR);
    }

    void setColor(Color color) {
        try {
            getConfiguration().setValue(Type.COLOR, color);
        } catch (ValidationException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        Paint oldPaint = g.getPaint();
        g.setPaint(getColor());
        Rectangle bounds = g.getClipBounds();
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setPaint(oldPaint);
    }

    public static class Type extends LayerType {

        private static final String COLOR = "color";

        @Override
        public String getName() {
            return "Background Layer";
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public ValueContainer getConfigurationTemplate() {
            final ValueContainer template = new ValueContainer();
            template.addModel(createDefaultValueModel(COLOR, Color.class));

            return template;

        }

        @Override
        protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
            return new BackgroundLayer(this, configuration);
        }
    }
}