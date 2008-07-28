package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImage;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;


public class SingleLevelImage implements LevelImage {
    private final PlanarImage planarImage;
    private final AffineTransform imageToModelTransform;
    private final AffineTransform modelToImageTransform;
    private final Rectangle2D boundingBox;

    public SingleLevelImage(RenderedImage image, AffineTransform imageToModelTransform) {
        final AffineTransform modelToImageTransform;
        try {
            modelToImageTransform = imageToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("imageToModelTransform", e);
        }
        this.planarImage = PlanarImage.wrapRenderedImage(image);
        this.imageToModelTransform = imageToModelTransform;
        this.modelToImageTransform = modelToImageTransform;
        this.boundingBox = imageToModelTransform.createTransformedShape(new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), planarImage.getHeight())).getBounds2D();
    }

    /**
     * @return Always 1.
     */
    @Override
    public int getLevelCount() {
        return 1;
    }

    /**
     * @return Always 0.
     */
    @Override
    public int computeLevel(double scale) {
        return 0;
    }

    /**
     * @return Always the one and only image.
     */
    @Override
    public final PlanarImage getPlanarImage(int level) {
        checkLevel(level);
        return planarImage;
    }

    @Override
    public void regenerateLevels(boolean removeCachedTiles) {
    }

    /**
     * @return Always the bounding box of the one and only image.
     */
    @Override
    public Rectangle2D getBounds(int level) {
        checkLevel(level);
        // for a MipMapMultiLevelImage, the bounding box is the same for all levels
        return new Rectangle2D.Double(boundingBox.getX(), boundingBox.getY(), boundingBox.getWidth(), boundingBox.getHeight());
    }

    /**
     * @return Always the one and only transformation.
     */
    @Override
    public final AffineTransform getImageToModelTransform(int level) {
        checkLevel(level);
        return new AffineTransform(imageToModelTransform);
    }

    /**
     * @return Always the one and only transformation.
     */
    @Override
    public final AffineTransform getModelToImageTransform(int level) {
        checkLevel(level);
        return new AffineTransform(modelToImageTransform);
    }

    private static void checkLevel(int level) {
        if (level != 0) {
            throw new IllegalArgumentException("level");
        }
    }
}
