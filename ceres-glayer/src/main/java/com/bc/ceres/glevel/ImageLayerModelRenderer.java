package com.bc.ceres.glevel;

import com.bc.ceres.grender.Rendering;

/**
 * An {@code ImageRenderer} is used to render multi-resolution {@link ImageLayerModel}s
 * at a certain level.
 */
public interface ImageLayerModelRenderer {
    /**
     * Renders a {@link ImageLayerModel} at the specified level onto the given {@link com.bc.ceres.grender.Rendering}.
     *
     * @param rendering  The rendering.
     * @param imageLayerModel The current level image.
     * @param level      the current resolution level.
     */
    void renderImage(Rendering rendering, ImageLayerModel imageLayerModel, int level);


    /**
     * Releases any allocated resources for {@code ImageRenderer}s, which maintain a state in between
     * calls to {@link #renderImage}.
     */
    void reset();
}