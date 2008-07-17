package com.bc.ceres.glayer.level;

import java.awt.image.RenderedImage;

/**
 * Allows to create a downscaled version of some {@link RenderedImage}.
 * <p/>
 * If this interface is implemented by a class implementing
 * the {@link RenderedImage} interface,  then the downscaled
 * image shall be of the same type as the implementing class.
 * <p/>
 * So, if such a class is named {@code A}, then
 * {@code A.class == new A().downscale(level).getClass()},
 * for all levels.
 */
public interface Downscaleable {
    /**
     * Creates a downscaled version of another {@code RenderedImage}.
     * @param level The resolution level.
     * @return The downscaled image.
     */
    RenderedImage downscale(int level);
}
