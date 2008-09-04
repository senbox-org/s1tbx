package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelModel;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

public class DefaultMultiLevelModel implements MultiLevelModel {

    private final int levelCount;
    private final AffineTransform[] imageToModelTransforms;
    private final AffineTransform[] modelToImageTransforms;
    private Rectangle2D modelBounds;

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
        int level = (int) Math.round(log2(scale));
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
            return new Rectangle2D.Double(
                    modelBounds.getX(), modelBounds.getY(),
                    modelBounds.getWidth(), modelBounds.getHeight());
        } else {
            return modelBounds;
        }
    }

    public void setModelBounds(Rectangle2D modelBounds) {
        if (modelBounds != null) {
            this.modelBounds = new Rectangle2D.Double(
                    modelBounds.getX(), modelBounds.getY(),
                    modelBounds.getWidth(), modelBounds.getHeight());
        } else {
            this.modelBounds = null;
        }

    }

    public static Rectangle2D getModelBounds(AffineTransform imageToModelTransform, RenderedImage levelZeroImage) {
        return imageToModelTransform.createTransformedShape(new Rectangle(levelZeroImage.getMinX(),
                                                                          levelZeroImage.getMinY(),
                                                                          levelZeroImage.getWidth(),
                                                                          levelZeroImage.getHeight())).getBounds2D();
    }

    public static Rectangle2D getModelBounds(AffineTransform imageToModelTransform, int w, int h) {
        return imageToModelTransform.createTransformedShape(new Rectangle(0, 0, w, h)).getBounds2D();
    }
}