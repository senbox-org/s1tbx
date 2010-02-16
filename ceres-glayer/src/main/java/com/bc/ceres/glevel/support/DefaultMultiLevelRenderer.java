package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Rendering;

import java.awt.geom.AffineTransform;

public class DefaultMultiLevelRenderer implements MultiLevelRenderer {

    public DefaultMultiLevelRenderer() {
    }

    @Override
    public void renderImage(Rendering rendering, MultiLevelSource multiLevelSource, int level) {
        final AffineTransform i2m = multiLevelSource.getModel().getImageToModelTransform(level);
        final AffineTransform m2v = rendering.getViewport().getModelToViewTransform();
        i2m.preConcatenate(m2v);
        rendering.getGraphics().drawRenderedImage(multiLevelSource.getImage(level), i2m);
    }

    @Override
    public void reset() {
        // no state to dispose
    }
}