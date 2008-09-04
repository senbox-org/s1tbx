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
     * Computes a resolution level from a given scaling factor, e.g. {@code level=(int)(-log(scale)/log(2)}).
     *
     * @param scale The scaling factor, will always be a greater than 0 and less than or equal to 1..
     * @return The resolution level, must be in the range 0 to {@link #getLevelCount()}-1.
     */
    int computeLevel(double scale);

    /**
     * Computes a scale from a given resolution level, e.g. {@code scale=pow(2,-level)}.
     *
     * @param level The resolution level.
     * @return The scaling factor, must be a positive number so that {@link #computeLevel(double scale)}
     *         can compute a valid level.
     */
    double computeScale(int level);

    /**
     * <p>Provides a hint that the level images provided so far will no longer be accessed from a
     * reference in user space.</p>
     *
     * <p>Therefore implementations of this method might also dispose any cached level images
     * that have been provided so far.</p>
     *
     * <p>After calling this method, a call to {@link #getLevelImage(int)}} for the same level may 
     * return a new level image instance.</p>
     *
     * <p>This method is particularly useful if properties have changed that affect the appearance of the
     * returned images at all levels, e.g. after a new color palette has been assigned or the
     * contrast range has changed.</p>
     */
    void reset();
}
