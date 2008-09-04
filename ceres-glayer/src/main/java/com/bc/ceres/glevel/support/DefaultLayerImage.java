package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LayerImage;
import com.bc.ceres.glevel.LevelImageSource;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public class DefaultLayerImage implements LayerImage {
    public static final DefaultLayerImage NULL = createNullImage();

    private final LevelImageSource levelImageSource;
    private final AffineTransform[] imageToModelTransforms;
    private final AffineTransform[] modelToImageTransforms;
    private Rectangle2D modelBounds;

    public DefaultLayerImage(LevelImageSource levelImageSource, AffineTransform imageToModelTransform, Rectangle2D modelBounds) {
        this.levelImageSource = levelImageSource;
        final AffineTransform modelToImageTransform;
        try {
            modelToImageTransform = imageToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("imageToModelTransform", e);
        }
        this.imageToModelTransforms = new AffineTransform[levelImageSource.getLevelCount()];
        this.modelToImageTransforms = new AffineTransform[levelImageSource.getLevelCount()];
        this.imageToModelTransforms[0] = new AffineTransform(imageToModelTransform);
        this.modelToImageTransforms[0] = new AffineTransform(modelToImageTransform);
        setModelBounds(modelBounds);
    }


    public LevelImageSource getLevelImageSource() {
        return levelImageSource;
    }

    @Override
    public final AffineTransform getImageToModelTransform(int level) {
        checkLevel(level);
        AffineTransform transform = imageToModelTransforms[level];
        if (transform == null) {
            transform = new AffineTransform(imageToModelTransforms[0]);
            final double s = getLevelImageSource().computeScale(level);
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

    protected void checkLevel(int level) {
        if (level < 0 || level >= getLevelImageSource().getLevelCount()) {
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

    private static DefaultLayerImage createNullImage() {
        return new DefaultLayerImage(new DefaultLevelImageSource(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY)), new AffineTransform(), new Rectangle(1, 1));
    }

}