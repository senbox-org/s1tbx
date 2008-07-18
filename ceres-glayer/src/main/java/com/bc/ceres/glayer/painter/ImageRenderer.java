package com.bc.ceres.glayer.painter;

import com.bc.ceres.glayer.Viewport;
import com.bc.ceres.glayer.Rendering;
import com.bc.ceres.glayer.level.LevelImage;

import java.awt.Graphics2D;

/**
 * An {@code ImageRenderer} is used to render multi-resolution {@link LevelImage}s
 * at a certain level.
 */
public interface ImageRenderer {
    /**
     * Renders a {@link LevelImage} at the specified level onto the given {@link Rendering}.
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