package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.grender.Rendering;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.util.Map;

/**
 * A background layer is used to draw a background using a unique {@link java.awt.Paint}.
 *
 * @author Norman Fomferra
 */
public class BackgroundLayer extends Layer {

    public BackgroundLayer(Paint paint) {
        super(LayerType.getLayerType(BackgroundLayer.class.getName()));
        getStyle().setProperty("paint", paint);
    }

    public Paint getPaint() {
        return (Paint) getStyle().getProperty("paint");
    }

    public void setPaint(Paint paint) {
        getStyle().setProperty("paint", paint);
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        Paint oldPaint = g.getPaint();
        g.setPaint(getPaint());
        Rectangle bounds = g.getClipBounds();
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setPaint(oldPaint);
    }
    
    public static class BackgroundLayerType extends LayerType {
        
        @Override
        public String getName() {
            return "Background Layer";
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public Map<String, Object> createConfiguration(LayerContext ctx, Layer layer) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Layer createLayer(LayerContext ctx, Map<String, Object> configuration) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}