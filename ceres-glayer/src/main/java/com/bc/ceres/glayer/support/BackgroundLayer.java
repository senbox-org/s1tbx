package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A background layer is used to draw a background using a unique {@link java.awt.Paint}.
 *
 * @author Norman Fomferra
 */
public class BackgroundLayer extends Layer {

    public BackgroundLayer(Paint paint) {
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
}