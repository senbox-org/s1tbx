package com.bc.ceres.glevel;

import java.awt.image.RenderedImage;

/**
 * {@link RenderedImage}s implementing this interface allow for creating a
 * downscaled version of itself.
 * <p/>
 * The returned downscaled image shall be of the same type as the implementing class.
 * Assuming the implementing class is named {@code A}, then
 * {@code A.class == new A().downscale(level).getClass()},
 * for all levels.
 */
public interface DownscalableImage extends RenderedImage {
    /**
     * Creates a downscaled version of this {@code RenderedImage}.
     *
     * @param level The resolution level.
     * @return The downscaled image.
     */
    RenderedImage downscale(int level);
}
