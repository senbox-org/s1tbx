package com.bc.ceres.glevel;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.glevel.LevelImage;

/**
 * An {@code ImageRenderer} is used to render multi-resolution {@link com.bc.ceres.glevel.LevelImage}s
 * at a certain level.
 */
public interface LevelImageRenderer {
    /**
     * Renders a {@link LevelImage} at the specified level onto the given {@link com.bc.ceres.grender.Rendering}.
     *
     * @param rendering  The rendering.
     * @param levelImage The current level image.
     * @param level      the current resolution level.
     */
    void renderImage(Rendering rendering, LevelImage levelImage, int level);

    /**
     * Releases any allocated resources for {@code ImageRenderer}s, which maintain a state in between
     * calls to {@link #renderImage}.
     */
    void dispose();
}