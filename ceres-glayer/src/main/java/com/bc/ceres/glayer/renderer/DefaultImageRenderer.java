package com.bc.ceres.glayer.renderer;

import com.bc.ceres.glayer.level.LevelImage;
import com.bc.ceres.grendering.Rendering;
import com.bc.ceres.grendering.Viewport;

import java.awt.geom.AffineTransform;
import java.awt.*;

public class DefaultImageRenderer implements ImageRenderer {

    public DefaultImageRenderer() {
    }

    public void renderImage(Rendering rendering, LevelImage levelImage, int level) {
        final AffineTransform t = new AffineTransform(levelImage.getImageToModelTransform(level));
        final Viewport viewport = rendering.getViewport();
        final Graphics2D graphics = rendering.getGraphics();
        t.preConcatenate(viewport.getModelToViewTransform());
        graphics.drawRenderedImage(levelImage.getPlanarImage(level), t);
    }

    public void dispose() {
        // no state to dispose
    }
}