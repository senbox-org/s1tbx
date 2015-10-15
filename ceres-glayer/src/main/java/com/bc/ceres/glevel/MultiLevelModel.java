/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.glevel;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

// todo - [multisize_products] add getImageLayout() to MultiLevelModel to force same image / tile sizes (nf 20151015)
// todo - [multisize_products] rename MultiLevelModel to MultiLevelImageLayout because MultiLevel*Model* and its getImageTo*Model*Transform are confusing
/**
 * The {@code MultiLevelModel} class represents a layout model for multi-resolution images such as image pyramids.
 * <p>It provides the number of resolution levels, the affine transformation
 * from image (pixel) to model coordinates and the bounds in model coordinates.
 * <p>The resolution level is an integer number ranging from zero (the highest resolution)
 * to {@link #getLevelCount()}-1 (the lowest resolution).
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 */
public interface MultiLevelModel {
    /**
     * Gets the number of resolution levels, which is always greater than zero.
     *
     * @return The number of resolution levels.
     */
    int getLevelCount();

    /**
     * Gets the resolution level for a given scaling factor, e.g. {@code level=log(scale)/log(2)}.
     *
     * @param scale The scaling factor, will always be a greater than or equal to 1.
     * @return The resolution level, must be in the range 0 to {@link #getLevelCount() levelCount}-1.
     * @see MultiLevelSource#getImage(int)
     */
    int getLevel(double scale);

    /**
     * Gets the scale for a given resolution level, e.g. {@code scale=pow(2,level)}.
     *
     * @param level The resolution level, must be in the range 0 to {@link #getLevelCount() levelCount}-1.
     * @return The scaling factor, must be greater than or equal to 1.
     *         {@link #getLevel(double) getLevel(scale)} shall return {@code level}.
     * @see MultiLevelSource#getImage(int)
     */
    double getScale(int level);

    /**
     * Gets a copy (non-life object) of the affine transformation from image to model coordinates for the given level.
     *
     * @param level The resolution level, must be in the range 0 to {@link #getLevelCount() levelCount}-1.
     * @return The affine transformation from image to model coordinates.
     */
    AffineTransform getImageToModelTransform(int level);

    /**
     * Gets a copy (non-life object) of the affine transformation from model to image coordinates for the given level.
     *
     * @param level The resolution level, must be in the range 0 to {@link #getLevelCount() levelCount}-1.
     * @return The affine transformation from model to image coordinates.
     */
    AffineTransform getModelToImageTransform(int level);

    /**
     * Gets the bounding box in model coordinates.
     *
     * @return The bounding box, may be {@code null} if unspecified.
     */
    Rectangle2D getModelBounds();
}