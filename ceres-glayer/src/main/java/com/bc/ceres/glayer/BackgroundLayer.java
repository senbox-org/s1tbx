package com.bc.ceres.glayer;

import com.bc.ceres.grendering.Rendering;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A background layer is used to draw a background using a unique {@link java.awt.Paint}.
 *
 * @author Norman Fomferra
 */
public class BackgroundLayer extends AbstractGraphicalLayer {

    private Paint paint;

    public BackgroundLayer(Paint paint) {
        this.paint = paint;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        Paint oldValue = this.paint;
        this.paint = paint;
        firePropertyChange("paint", oldValue, this.paint);
    }

    public Rectangle2D getBoundingBox() {
        return null;
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        Paint oldPaint = g.getPaint();
        g.setPaint(paint);
        Rectangle bounds = g.getClipBounds();
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setPaint(oldPaint);
    }
}