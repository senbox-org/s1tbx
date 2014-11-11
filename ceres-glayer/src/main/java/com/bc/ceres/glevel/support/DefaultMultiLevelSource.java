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

import javax.media.jai.Interpolation;
import javax.media.jai.OpImage;
import javax.media.jai.TileCache;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

/**
 * A default implementation for the {@link MultiLevelSource} interface.
 */
public class DefaultMultiLevelSource extends AbstractMultiLevelSource {

    /**
     * Default interpolation is "Nearest Neighbour".
     */
    public static final Interpolation DEFAULT_INTERPOLATION = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
    public static final MultiLevelSource NULL = createNull();

    private final RenderedImage sourceImage;
    private final Interpolation interpolation;

    /**
     * Constructs a new instance with {@link #DEFAULT_INTERPOLATION}.
     *
     * @param sourceImage The source image.
     * @param levelCount  The level count.
     */
    public DefaultMultiLevelSource(RenderedImage sourceImage, int levelCount) {
        this(sourceImage, levelCount, DEFAULT_INTERPOLATION);
    }

    /**
     * Constructs a new instance.
     *
     * @param sourceImage   The source image.
     * @param levelCount    The level count.
     * @param interpolation The interpolation.
     */
    public DefaultMultiLevelSource(RenderedImage sourceImage, int levelCount, Interpolation interpolation) {
        this(sourceImage, createDefaultMultiLevelModel(sourceImage, levelCount), interpolation);
    }

    /**
     * Constructs a new instance with {@link #DEFAULT_INTERPOLATION}.
     *
     * @param sourceImage     The source image.
     * @param multiLevelModel The multi level model.
     */
    public DefaultMultiLevelSource(RenderedImage sourceImage, MultiLevelModel multiLevelModel) {
        this(sourceImage, multiLevelModel, DEFAULT_INTERPOLATION);
    }

    /**
     * Constructs a new instance with {@link #DEFAULT_INTERPOLATION}.
     *
     * @param sourceImage     The source image.
     * @param multiLevelModel The multi level model.
     * @param interpolation   The interpolation.
     */
    public DefaultMultiLevelSource(RenderedImage sourceImage, MultiLevelModel multiLevelModel, Interpolation interpolation) {
        super(multiLevelModel);
        this.sourceImage = sourceImage;
        this.interpolation = interpolation;
    }

    public RenderedImage getSourceImage() {
        return sourceImage;
    }

    public Interpolation getInterpolation() {
        return interpolation;
    }

    /**
     * Returns the level-0 image if {@code level} equals zero, otherwise calls {@code super.getLevelImage(level)}.
     * This override prevents the base class from storing a reference to the source image (the level-0 image).
     * See {@link AbstractMultiLevelSource#createImage(int)}.
     *
     * @param level The level.
     * @return The image.
     */
    @Override
    public synchronized RenderedImage getImage(int level) {
        if (level == 0) {
            return sourceImage;
        }
        return super.getImage(level);
    }

    /**
     * Creates a scaled version of the level-0 image for the given level.
     * See {@link #getImage(int)} and {@link AbstractMultiLevelSource#createImage(int) super.createImage(int)}.
     *
     * @param level The level.
     * @return The image.
     */
    @Override
    protected RenderedImage createImage(int level) {

        if (level == 0) {
            return sourceImage;
        }

        double scale = getModel().getScale(level);
        double invScale = 1.0 / scale;
        int jaiW = getLevelImageSizeJAI(sourceImage.getWidth(), scale);
        int jaiH = getLevelImageSizeJAI(sourceImage.getHeight(), scale);
        int j2kW = getLevelImageSizeJ2K(sourceImage.getWidth(), scale);
        int j2kH = getLevelImageSizeJ2K(sourceImage.getHeight(), scale);

        // Force JAI ScaleDescriptor to compute J2K-sized lower resolution images

        float scaleX;
        if (jaiW == j2kW) {
            scaleX = (float) invScale;
        } else {
            scaleX = (float) ((double) j2kW / (double) sourceImage.getWidth());
        }

        float scaleY;
        if (jaiH == j2kH) {
            scaleY = (float) invScale;
        } else {
            scaleY = (float) ((double) j2kH  /(double) sourceImage.getHeight());
        }

        return ScaleDescriptor.create(sourceImage, scaleX, scaleY, 0.0F, 0.0F, interpolation, null);
    }

    @Override
    public void reset() {
        removeTilesFromCache(sourceImage);
        super.reset();
    }

    public static MultiLevelModel createDefaultMultiLevelModel(RenderedImage sourceImage, int levelCount) {
        return new DefaultMultiLevelModel(levelCount,
                                          new AffineTransform(),
                                          sourceImage.getWidth(),
                                          sourceImage.getHeight());
    }


    /**
     * Computes the boundaries of an image at a given resolution scaling from the given source image boundaries (at level zero).
     *
     * @param sourceBounds The image boundaries of the level zero image.
     * @param scale        The scale at a given level as returned by {@link MultiLevelModel#getScale(int)}.
     * @return The level image boundaries in pixel coordinates.
     * @since BEAM 5
     */
    public static Rectangle getLevelImageBounds(Rectangle sourceBounds, double scale) {
        return new Rectangle(getLevelImageSizeJ2K(sourceBounds.x, scale),
                             getLevelImageSizeJ2K(sourceBounds.y, scale),
                             getLevelImageSizeJ2K(sourceBounds.width, scale),
                             getLevelImageSizeJ2K(sourceBounds.height, scale));
    }

    // JPEG2000 Style:
    // Used in order to support S-2 MSI image data.
    // Will ensure that no data loss takes place due to truncation.
    // Return value will always be >= 1.
    private static int getLevelImageSizeJ2K(int sourceSize, double scale) {
        return (int) Math.ceil(sourceSize / scale);
    }

    // JAI ScaleDescriptor Style:
    // Return value may be zero.
    private static int getLevelImageSizeJAI(int sourceSize, double scale) {
        return (int) Math.ceil(sourceSize / scale - 0.5);
    }

    private static MultiLevelSource createNull() {
        final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        final DefaultMultiLevelModel model = new DefaultMultiLevelModel(1, new AffineTransform(), null);
        return new DefaultMultiLevelSource(image, model);
    }

    // todo - very useful method, make it accessible from outside.
    private static void removeTilesFromCache(RenderedImage image) {
        if (image instanceof OpImage) {
            OpImage opImage = (OpImage) image;
            TileCache tileCache = opImage.getTileCache();
            if (tileCache != null) {
                tileCache.removeTiles(image);
            }
        }
    }
}
