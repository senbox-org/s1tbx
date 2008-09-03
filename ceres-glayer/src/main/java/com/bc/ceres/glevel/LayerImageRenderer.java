package com.bc.ceres.glevel;

import com.bc.ceres.grender.Rendering;

/**
 * An {@code ImageRenderer} is used to render multi-resolution {@link com.bc.ceres.glevel.LayerImage}s
 * at a certain level.
 */
public interface LayerImageRenderer {
    /**
     * Renders a {@link LayerImage} at the specified level onto the given {@link com.bc.ceres.grender.Rendering}.
     *
     * @param rendering  The rendering.
     * @param layerImage The current level image.
     * @param level      the current resolution level.
     */
    void renderImage(Rendering rendering, LayerImage layerImage, int level);


    /**
     * Releases any allocated resources for {@code ImageRenderer}s, which maintain a state in between
     * calls to {@link #renderImage}.
     */
    void reset();
}