package com.bc.ceres.glevel;

import java.awt.image.RenderedImage;

/**
 * A source for images at a given resolution level.
 * The resolution level, an integer number ranging from zero (the highest resolution)
 * to {@link #getLevelCount()}-1 (the lowest resolution) is computed from a given
 * scaling factor and vice versa.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @version $revision$ $date$
 */
public interface LevelImageSource {
    /**
     * Gets the number of resolution levels, which is always greater than zero.
     *
     * @return The number of resolution levels.
     */
    int getLevelCount();

    /**
     * Gets the scaled image for the given resolution level.
     *
     * @param level The resolution level.
     * @return The scaled image, must be in the range 0 to {@link #getLevelCount()}-1.
     */
    RenderedImage getLevelImage(int level);

    /**
     * Computes a resolution level from a given scaling factor, e.g. {@code level=(int)(log(scale)/log(2)}).
     *
     * @param scale The scaling factor, will always be a positive number.
     * @return The resolution level, must be in the range 0 to {@link #getLevelCount()}-1.
     */
    int computeLevel(double scale);

    /**
     * Computes a scale from a given resolution level, e.g. {@code scale=pow(2,level)}.
     *
     * @param level The resolution level.
     * @return The scaling factor, must be a positive number so that {@link #computeLevel(double scale)}
     *         can compute a valid level.
     */
    double computeScale(int level);
}
