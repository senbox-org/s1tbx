package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LayerImage;
import com.bc.ceres.glevel.LayerImageRenderer;
import com.bc.ceres.grender.Rendering;

import java.awt.geom.AffineTransform;

public class DefaultLayerImageRenderer implements LayerImageRenderer {

    public DefaultLayerImageRenderer() {
    }

    @Override
    public void renderImage(Rendering rendering, LayerImage layerImage, int level) {
        final AffineTransform i2m = layerImage.getImageToModelTransform(level);
        final AffineTransform m2v = rendering.getViewport().getModelToViewTransform();
        i2m.preConcatenate(m2v);
        rendering.getGraphics().drawRenderedImage(layerImage.getLevelImage(level), i2m);
    }

    @Override
    public void reset() {
        // no state to dispose
    }
}