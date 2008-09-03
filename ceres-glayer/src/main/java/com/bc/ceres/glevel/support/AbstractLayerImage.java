package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LayerImage;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.Rectangle;

public abstract class AbstractLayerImage implements LayerImage {

    private final int levelCount;
    private final AffineTransform[] imageToModelTransforms;
    private final AffineTransform[] modelToImageTransforms;
    private Rectangle2D modelBounds;

    public AbstractLayerImage(AffineTransform imageToModelTransform, int levelCount) {
        final AffineTransform modelToImageTransform;
        try {
            modelToImageTransform = imageToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("imageToModelTransform", e);
        }
        this.levelCount = levelCount;
        this.imageToModelTransforms = new AffineTransform[levelCount];
        this.modelToImageTransforms = new AffineTransform[levelCount];
        this.imageToModelTransforms[0] = new AffineTransform(imageToModelTransform);
        this.modelToImageTransforms[0] = new AffineTransform(modelToImageTransform);
    }

    @Override
    public int getLevelCount() {
        return levelCount;
    }

    @Override
    public int computeLevel(double scale) {
        int level = (int) Math.round(log2(scale));
        if (level < 0) {
            level = 0;
        } else if (level >= levelCount) {
            level = levelCount - 1;
        }
        return level;
    }

    @Override
    public double computeScale(int level) {
        return pow2(level);
    }
    
    /**
     * Removes all cached images and also removes all cached tiles of those images
     * from the JAI tile cche.
     */
    @Override
    public void reset() {
    }

    @Override
    public final AffineTransform getImageToModelTransform(int level) {
        checkLevel(level);
        AffineTransform transform = imageToModelTransforms[level];
        if (transform == null) {
            transform = new AffineTransform(imageToModelTransforms[0]);
            final double s = computeScale(level);
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



    @Override
    public Rectangle2D getModelBounds() {
        return new Rectangle2D.Double(
                modelBounds.getX(), modelBounds.getY(),
                modelBounds.getWidth(), modelBounds.getHeight());
    }


    public void setModelBounds(Rectangle2D modelBounds) {
        this.modelBounds = new Rectangle2D.Double(
                modelBounds.getX(), modelBounds.getY(),
                modelBounds.getWidth(), modelBounds.getHeight());
    }

    protected static double pow2(double v) {
        return Math.pow(2.0, v);
    }

    protected static double log2(double v) {
        return Math.log(v) / Math.log(2.0);
    }

    protected void checkLevel(int level) {
        if (level < 0 || level >= levelCount) {
            throw new IllegalArgumentException("level");
        }
    }
    // TODO move somewhere else ???
    public static Rectangle2D getModelBounds(AffineTransform imageToModelTransform, RenderedImage levelZeroImage) {
        return imageToModelTransform.createTransformedShape(new Rectangle(levelZeroImage.getMinX(),
                                                                                 levelZeroImage.getMinY(),
                                                                                 levelZeroImage.getWidth(),
                                                                                 levelZeroImage.getHeight())).getBounds2D();
    }

    // TODO move somewhere else ???
    public static Rectangle2D getModelBounds(AffineTransform imageToModelTransform, int w, int h) {
        return imageToModelTransform.createTransformedShape(new Rectangle(0, 0, w, h)).getBounds2D();
    }
}