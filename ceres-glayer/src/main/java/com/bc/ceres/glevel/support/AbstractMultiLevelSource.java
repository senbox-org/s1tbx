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

package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.RenderedImage;

/**
 * An abstract base class for {@link MultiLevelSource} implementations.
 * Level images are cached unless {@link #reset()} is called.
 * Subclasses are asked to implement {@link #createImage(int)}.
 */
public abstract class AbstractMultiLevelSource implements MultiLevelSource {

    private final MultiLevelModel multiLevelModel;
    private final RenderedImage[] levelImages;

    protected AbstractMultiLevelSource(MultiLevelModel multiLevelModel) {
        this.multiLevelModel = multiLevelModel;
        this.levelImages = new RenderedImage[multiLevelModel.getLevelCount()];
    }

    @Override
    public MultiLevelModel getModel() {
        return multiLevelModel;
    }

    /**
     * Gets the {@code RenderedImage} at the given resolution level. Unless {@link #reset()} is called,
     * the method will always return the same image instance at the same resolution level.
     * If a level image is requested for the first time, the method calls
     * {@link #createImage(int)} in order to retrieve the actual image instance.
     *
     * @param level The resolution level.
     * @return The {@code RenderedImage} at the given resolution level.
     */
    @Override
    public synchronized RenderedImage getImage(int level) {
        checkLevel(level);
        RenderedImage levelImage = levelImages[level];
        if (levelImage == null) {
            levelImage = createImage(level);
            levelImages[level] = levelImage;
        }
        return levelImage;
    }

    @Override
    public Shape getImageShape(int level) {
        return null;
    }

    /**
     * Called by {@link #getImage(int)} if a level image is requested for the first time.
     * Note that images created via this method will be {@link PlanarImage#dispose disposed}
     * when {@link #reset} is called on this multi-level image source. See {@link #getImage(int)}.
     * <p>
     * The dimension of the level image created must be the same as that obtained from
     * {@link #getImageDimension(int, int, double)} for the scale associated with the
     * given resolution level.
     *
     * @param level The resolution level.
     * @return An instance of a {@code RenderedImage} for the given resolution level.
     */
    protected abstract RenderedImage createImage(int level);


    /**
     * Removes all cached level images and also disposes
     * any {@link javax.media.jai.PlanarImage PlanarImage}s among them.
     * <p>Overrides should always call {@code super.reset()}.<p>
     */
    @Override
    public synchronized void reset() {
        for (int level = 0; level < levelImages.length; level++) {
            RenderedImage levelImage = levelImages[level];
            if (levelImage instanceof PlanarImage) {
                PlanarImage planarImage = (PlanarImage) levelImage;
                planarImage.dispose();
            }
            levelImages[level] = null;
        }
    }

    /**
     * Utility method which checks if a given level is valid.
     *
     * @param level The resolution level.
     * @throws IllegalArgumentException if {@code level &lt; 0 || level &gt;= getModel().getLevelCount()}
     */
    protected synchronized void checkLevel(int level) {
        if (level < 0 || level >= getModel().getLevelCount()) {
            throw new IllegalArgumentException("level=" + level);
        }
    }

    /**
     * Computes the dimension of an image at a certain level. The image dimension computed is the
     * same as that obtained from {@code javax.media.jai.operator.ScaleDescriptor.create(...)}.
     *
     * @param width  The width of the image in pixels at level zero.
     * @param height The height of the image in pixels at level zero.
     * @param scale  The scale at the level of interest.
     * @return the dimension of the image at the level of interest.
     * @deprecated since Ceres 0.14, lower-level resolutions of image pyramids are computed in the JPEG2000-style,
     * that is {@code newSize=(int)ceil(scale * size)} (ceiling integer),
     * while JAI is {@code newSize=(int)ceil(scale * size - 0.5)} (rounding to nearest integer).
     * Please use {@link DefaultMultiLevelSource#getImageRectangle(int, int, int, int, double)} instead.
     */
    @Deprecated
    public static Dimension getImageDimension(int width, int height, double scale) {
        final float scaleFactor = (float) (1.0 / scale);
        final RenderedOp c = ConstantDescriptor.create((float) width, (float) height, new Float[]{0.0f}, null);
        final RenderedOp s = ScaleDescriptor.create(c, scaleFactor, scaleFactor, 0.0f, 0.0f, null, null);
        return new Dimension(s.getWidth(), s.getHeight());
    }

    /**
     * Computes the rectangle of an image at a certain level. The image rectangle computed is the
     * same as that obtained from {@code javax.media.jai.operator.ScaleDescriptor.create(...)}.
     *
     * @param minX   The image's minimum X coordinate in pixels at level zero.
     * @param minY   The image's minimum Y coordinate in pixels at level zero.
     * @param width  The width of the image in pixels at level zero.
     * @param height The height of the image in pixels at level zero.
     * @param scale  The scale at the level of interest.
     * @return the dimension of the image at the level of interest.
     * @deprecated since Ceres 0.14, lower-level resolutions of image pyramids are computed in the JPEG2000-style,
     * that is {@code newSize=(int)ceil(scale * size)} (ceiling integer),
     * while JAI is {@code newSize=(int)ceil(scale * size - 0.5)} (rounding to nearest integer).
     * Please use {@link DefaultMultiLevelSource#getImageRectangle(int, int, int, int, double)} instead.
     */
    @Deprecated
    public static Rectangle getImageRectangle(int minX, int minY, int width, int height, double scale) {
        final float scaleFactor = (float) (1.0 / scale);
        final RenderedOp c = ConstantDescriptor.create((float) width, (float) height, new Float[]{0.0f}, null);
        final RenderedOp s1 = ScaleDescriptor.create(c, 1.0F, 1.0F, (float) minX, (float) minY, null, null);
        final RenderedOp s2 = ScaleDescriptor.create(s1, scaleFactor, scaleFactor, 0.0F, 0.0F, null, null);
        return new Rectangle(s2.getMinX(), s2.getMinY(), s2.getWidth(), s2.getHeight());
    }
}
