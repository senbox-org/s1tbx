package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.ImageLayerModel;
import com.bc.ceres.glevel.ImageLayerModelRenderer;
import com.bc.ceres.grender.Rendering;

import java.awt.geom.AffineTransform;

public class DefaultImageLayerModelRenderer implements ImageLayerModelRenderer {

    public DefaultImageLayerModelRenderer() {
    }

    @Override
    public void renderImage(Rendering rendering, ImageLayerModel imageLayerModel, int level) {
        final AffineTransform i2m = imageLayerModel.getImageToModelTransform(level);
        final AffineTransform m2v = rendering.getViewport().getModelToViewTransform();
        i2m.preConcatenate(m2v);
        rendering.getGraphics().drawRenderedImage(imageLayerModel.getLevelImageSource().getLevelImage(level), i2m);
    }

    @Override
    public void reset() {
        // no state to dispose
    }
}