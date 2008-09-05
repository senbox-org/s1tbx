package com.bc.ceres.glevel;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The {@code MultiLevelModel} class represents a layout model for multi-resolution images such as image pyramids.
 * <p>It comprises the number of resolution levels, the affine transformation
 * from image (pixel) to model coordinates and the bounds in model coordinates.</p>
 * <p>The resolution level is an integer number ranging from zero (the highest resolution)
 * to {@link #getLevelCount()}-1 (the lowest resolution).</p>
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 * @version $revision$ $date$
 */
public interface MultiLevelModel {
    /**
     * Gets the number of resolution levels, which is always greater than zero.
     *
     * @return The number of resolution levels.
     */
    int getLevelCount();

    /**
     * Computes a resolution level from a given scaling factor, e.g. {@code level=(int)(-log(scale)/log(2)}).
     *
     * @param scale The scaling factor, will always be a greater than 0 and less than or equal to 1..
     * @return The resolution level, must be in the range 0 to {@link #getLevelCount()}-1.
     */
    int getLevel(double scale);

    /**
     * Computes a scale from a given resolution level, e.g. {@code scale=pow(2,-level)}.
     *
     * @param level The resolution level.
     * @return The scaling factor, must be a positive number so that {@link #computeLevel(double scale)}
     *         can compute a valid level.
     */
    double getScale(int level);

    /**
     * Gets a copy (non-life object) of the affine transformation from image to model coordinates for the given level.
     *
     * @param level the resolution level
     * @return the affine transformation from image to model coordinates.
     */
    AffineTransform getImageToModelTransform(int level);

    /**
     * Gets a copy (non-life object) of the affine transformation from model to image coordinates for the given level.
     *
     * @param level the resolution level
     * @return the affine transformation from model to image coordinates.
     */
    AffineTransform getModelToImageTransform(int level);

    /**
     * Returns the bounding box in model coordinates.
     *
     * @return the bounding box.
     */
    Rectangle2D getModelBounds();
}