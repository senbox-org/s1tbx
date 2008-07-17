package com.bc.ceres.glayer.painter;

import com.bc.ceres.glayer.Viewport;
import com.bc.ceres.glayer.level.LevelImage;

import java.awt.Graphics2D;

/**
 * An {@code ImagePainter} is used to paint multi-resolution {@link LevelImage}s
 * at a certain level.
 */
public interface ImagePainter {
    /**
     * Paints a {@link LevelImage} at the given resolution level
     * on the given {@link Graphics2D} context using the given {@link Viewport}.
     *
     * @param graphics   The current graphics context.
     * @param viewport   The current viewport.
     * @param levelImage The current level image.
     * @param level      the current resolution level.
     */
    void paint(Graphics2D graphics, Viewport viewport, LevelImage levelImage, int level);

    /**
     * Releases any allocated resources for {@code ImagePainter}s which maintain a state during calls
     * to {@link #paint}.
     */
    void dispose();
}