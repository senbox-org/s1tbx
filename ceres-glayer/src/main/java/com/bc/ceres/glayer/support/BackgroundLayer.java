package com.bc.ceres.glayer.support;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Color;

/**
 * A background layer is used to draw a background using a unique {@link java.awt.Paint}.
 *
 * @author Norman Fomferra
 */
public class BackgroundLayer extends Layer {

    private static final Type LAYER_TYPE = (Type) LayerType.getLayerType(Type.class.getName());

    public BackgroundLayer(Color paint) {
        this(LAYER_TYPE, paint);
    }

    protected BackgroundLayer(Type type, Color color) {
        super(type);
        setColor(color);
    }

    public Color getColor() {
        return (Color) getStyle().getProperty("color");
    }

    public void setColor(Color color) {
        getStyle().setProperty("color", color);
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
            template.addModel(createDefaultValueModel("color", Color.class));

            return template;

        }

        @Override
        protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
            Color color = (Color) configuration.getValue("color");
            return new BackgroundLayer(color);
        }
    }
}