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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * A default implementation for a the {@link MultiLevelModel} interface.
 */
public class DefaultMultiLevelModel implements MultiLevelModel {

    public final static int DEFAULT_MAX_LEVEL_PIXEL_COUNT = 256 * 256;

    private final int levelCount;
    private final AffineTransform[] imageToModelTransforms;
    private final AffineTransform[] modelToImageTransforms;
    private Rectangle2D modelBounds;

    /**
     * Constructs a a new model for a multi level source.
     * The number of levels is computed by {@link #getLevelCount(int, int)}.
     * The image bounds are computed by {@link #getModelBounds(java.awt.geom.AffineTransform, int, int)}.
     *
     * @param imageToModelTransform The affine transformation from image to model coordinates.
     * @param width                 The width of the image in pixels at level zero.
     * @param height                The height of the image in pixels at level zero.
     */
    public DefaultMultiLevelModel(AffineTransform imageToModelTransform,
                                  int width, int height) {
        this(getLevelCount(width, height), imageToModelTransform, getModelBounds(imageToModelTransform, width, height));
    }

    /**
     * Constructs a a new model for a multi level source.
     * The image bounds are computed by {@link #getModelBounds(java.awt.geom.AffineTransform, int, int)}.
     *
     * @param levelCount            The number of levels.
     * @param imageToModelTransform The affine transformation from image to model coordinates.
     * @param width                 The width of the image in pixels at level zero.
     * @param height                The height of the image in pixels at level zero.
     */
    public DefaultMultiLevelModel(int levelCount,
                                  AffineTransform imageToModelTransform,
                                  int width, int height) {
        this(levelCount, imageToModelTransform, getModelBounds(imageToModelTransform, width, height));
    }

    /**
     * Constructs a a new model for a multi level source.
     *
     * @param levelCount            The number of levels.
     * @param imageToModelTransform The affine transformation from image to model coordinates.
     * @param modelBounds           The image bounds in model coordinates.
     */
    public DefaultMultiLevelModel(int levelCount,
                                  AffineTransform imageToModelTransform,
                                  Rectangle2D modelBounds) {
        this.levelCount = levelCount;
        final AffineTransform modelToImageTransform;
        try {
            modelToImageTransform = imageToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("imageToModelTransform", e);
        }
        this.imageToModelTransforms = new AffineTransform[levelCount];
        this.modelToImageTransforms = new AffineTransform[levelCount];
        this.imageToModelTransforms[0] = new AffineTransform(imageToModelTransform);
        this.modelToImageTransforms[0] = new AffineTransform(modelToImageTransform);
        setModelBounds(modelBounds);
    }


    @Override
    public int getLevelCount() {
        return levelCount;
    }

    @Override
    public int getLevel(double scale) {
        int level = (int) Math.floor(log2(scale));
        if (level < 0) {
            level = 0;
        } else if (level >= levelCount) {
            level = levelCount - 1;
        }
        return level;
    }

    @Override
    public double getScale(int level) {
        checkLevel(level);
        return pow2(level);
    }


    @Override
    public final AffineTransform getImageToModelTransform(int level) {
        checkLevel(level);
        AffineTransform transform = imageToModelTransforms[level];
        if (transform == null) {
            transform = new AffineTransform(imageToModelTransforms[0]);
            final double s = getScale(level);
            transform.scale(s, s);
            imageToModelTransforms[level] = transform;
        }
        return new AffineTransform(transform);
    }


    @Override
    public final AffineTransform getModelToImageTransform(int level) {
        checkLevel(level);
        AffineTransform transform = modelToImageTransforms[level];
        if (transform == null) {
            try {
                transform = getImageToModelTransform(level).createInverse();
                modelToImageTransforms[level] = transform;
            } catch (NoninvertibleTransformException e) {
                throw new IllegalStateException(e);
            }
        }
        return new AffineTransform(transform);
    }

    protected static double pow2(double v) {
        return Math.pow(2.0, v);
    }

    protected static double log2(double v) {
        return Math.log(v) / Math.log(2.0);
    }

    protected void checkLevel(int level) {
        if (level < 0 || level >= getLevelCount()) {
            throw new IllegalArgumentException("level");
        }
    }

    @Override
    public Rectangle2D getModelBounds() {
        if (modelBounds != null) {
            return (Rectangle2D) modelBounds.clone();
        } else {
            return null;
        }
    }

    public void setModelBounds(Rectangle2D modelBounds) {
        if (modelBounds != null) {
            this.modelBounds = (Rectangle2D) modelBounds.clone();
        } else {
            this.modelBounds = null;
        }
    }

    /**
     * Computes the image bounding box in model coordinates.
     *
     * @param i2mTransform   The affine transformation from image to model coordinates.
     * @param levelZeroImage The image at level zero.
     * @return The number of levels.
     */
    public static Rectangle2D getModelBounds(AffineTransform i2mTransform, RenderedImage levelZeroImage) {
        return i2mTransform.createTransformedShape(new Rectangle(levelZeroImage.getMinX(),
                                                                 levelZeroImage.getMinY(),
                                                                 levelZeroImage.getWidth(),
                                                                 levelZeroImage.getHeight())).getBounds2D();
    }

    /**
     * Computes the image bounding box in model coordinates.
     *
     * @param i2mTransform The affine transformation from image to model coordinates.
     * @param width        The width of the image in pixels at level zero.
     * @param height       The height of the image in pixels at level zero.
     * @return The number of levels.
     */
    public static Rectangle2D getModelBounds(AffineTransform i2mTransform, int width, int height) {
        return i2mTransform.createTransformedShape(new Rectangle(0, 0, width, height)).getBounds2D();
    }

    /**
     * Computes the number of levels using the {@link #DEFAULT_MAX_LEVEL_PIXEL_COUNT} constant.
     *
     * @param width  The width of the image in pixels at level zero.
     * @param height The height of the image in pixels at level zero.
     * @return The number of levels.
     */
    public static int getLevelCount(int width, int height) {
        int levelCount = 1;
        double scale = 1.0;
        while ((scale * width) * (scale * height) >= DEFAULT_MAX_LEVEL_PIXEL_COUNT) {
            levelCount++;
            scale *= 0.5;
        }
        return levelCount;
    }
}