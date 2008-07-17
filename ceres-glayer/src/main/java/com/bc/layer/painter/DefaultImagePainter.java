package com.bc.layer.painter;

import com.bc.layer.Viewport;
import com.bc.layer.level.LevelImage;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class DefaultImagePainter implements ImagePainter {

    public DefaultImagePainter() {
    }

    public void paint(Graphics2D graphics, Viewport viewport, LevelImage levelImage, int level) {
        final AffineTransform t = new AffineTransform(levelImage.getImageToModelTransform(level));
        t.preConcatenate(viewport.getModelToViewTransform());
        graphics.drawRenderedImage(levelImage.getPlanarImage(level), t);
    }

    public void dispose() {
        // no state to dispose
    }
}