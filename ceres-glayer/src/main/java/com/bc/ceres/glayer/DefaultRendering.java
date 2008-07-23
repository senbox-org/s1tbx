package com.bc.ceres.glayer;

import com.bc.ceres.core.Assert;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A default implementation of the {@link Rendering} interface.
 */
public class DefaultRendering implements Rendering {
    private Viewport viewport;
    private Graphics2D graphics;

    public DefaultRendering(Viewport viewport, Graphics2D graphics) {
        setViewport(viewport);
        setGraphics(graphics);
    }

    public Rectangle2D getBounds() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void setViewport(Viewport viewport) {
        Assert.notNull(viewport, "viewport");
        this.viewport = viewport;
    }

    public Graphics2D getGraphics() {
        return graphics;
    }

    public void setGraphics(Graphics2D graphics) {
        Assert.notNull(graphics, "graphics");
        this.graphics = graphics;
    }
}