package com.bc.ceres.glevel;

import java.awt.image.RenderedImage;

/**
  * {@link RenderedImage}s implementing this interface allow for creating a
 * lower resolution version of itself.
 * <p/>
 * The returned image shall be of the same type as the implementing class.
 * Assuming the implementing class is named {@code A}, then
 * {@code A.class == new A().downscale(level).getClass()},
 * for all levels.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public interface MRImage extends RenderedImage {
    RenderedImage getLRImage(int level);
    void dispose();
}
