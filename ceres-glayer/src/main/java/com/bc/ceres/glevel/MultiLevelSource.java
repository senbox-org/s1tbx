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

import com.bc.ceres.glevel.support.DefaultMultiLevelSource;

import java.awt.Shape;
import java.awt.image.RenderedImage;

/**
 * A source for images at a given resolution level. The layout of the {@code MultiLevelSource}
 * is given by a {@link MultiLevelModel} which also provides the number of available resolution
 * levels.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @version $revision$ $date$
 */
public interface MultiLevelSource {
    MultiLevelSource NULL = DefaultMultiLevelSource.NULL;

    /**
     * Gets the layout model for the multi-resolution image supported by this {@code LevelImageSource}.
     *
     * @return the multi-resolution image model.
     */
    MultiLevelModel getModel();

    /**
     * Gets the scaled image for the given resolution level.
     * The width and height of an image returned for a given {@code level} is
     * {@link MultiLevelModel#getScale(int) scale} times smaller than the dimensions of the
     * image at {@code level=0}.
     *
     * @param level The resolution level.
     * @return The scaled image, must be in the range 0 to {@link MultiLevelModel#getLevelCount()}-1.
     */
    RenderedImage getImage(int level);

    /**
     * Gets the shape of the area where this image's raster data contains valid pixels at the given resolution level.
     * The method returns <code>null</code>, if the entire image raster contains valid pixels.
     *
     * @param level The resolution level.
     * @return The shape of the area where the image has data, can be {@code null}.
     */
    Shape getImageShape(int level);

    /**
     * <p>Provides a hint that the level images provided so far will no longer be accessed from a
     * reference in user space.
     * <p>Therefore implementations of this method might also dispose any cached level images
     * that have been provided so far.
     * <p>After calling this method, a call to {@link #getImage(int)}} for the same level may
     * return a new level image instance.
     * <p>This method is particularly useful if properties have changed that affect the appearance of the
     * returned images at all levels, e.g. after a new color palette has been assigned or the
     * contrast range has changed.
     */
    void reset();
}
