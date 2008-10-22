package com.bc.ceres.grender.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import java.awt.*;

/**
 * A default implementation of the {@link com.bc.ceres.grender.Rendering} interface.
 */
public class DefaultRendering implements Rendering {
    private Graphics2D graphics;
    private Viewport viewport;

    public DefaultRendering(Graphics2D graphics, Viewport viewport) {
        setViewport(viewport);
        setGraphics(graphics);
    }

    @Override
    public Graphics2D getGraphics() {
        return graphics;
    }

    public void setGraphics(Graphics2D graphics) {
        Assert.notNull(graphics, "graphics");
        this.graphics = graphics;
    }

    @Override
    public Viewport getViewport() {
        return viewport;
    }

    public void setViewport(Viewport viewport) {
        Assert.notNull(viewport, "viewport");
        this.viewport = viewport;
    }
}