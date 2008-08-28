package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.DownscalableImage;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

public class DefaultMultiLevelImage extends AbstractMultiLevelImage {

    private final PlanarImage levelZeroImage;
    private final Rectangle2D boundingBox;
    private final Interpolation interpolation;

    public DefaultMultiLevelImage(RenderedImage levelZeroImage,
                                  AffineTransform imageToModelTransform,
                                  int levelCount) {
        this(levelZeroImage,
             imageToModelTransform,
             levelCount,
             Interpolation.getInstance(Interpolation.INTERP_BICUBIC));
    }

    public DefaultMultiLevelImage(RenderedImage levelZeroImage,
                                  AffineTransform imageToModelTransform,
                                  int levelCount,
                                  Interpolation interpolation) {
        super(imageToModelTransform, levelCount);
        boundingBox = imageToModelTransform.createTransformedShape(new Rectangle(levelZeroImage.getMinX(),
                                                                                 levelZeroImage.getMinY(),
                                                                                 levelZeroImage.getWidth(),
                                                                                 levelZeroImage.getHeight())).getBounds2D();
        this.levelZeroImage = PlanarImage.wrapRenderedImage(levelZeroImage);
        this.interpolation = interpolation;
    }

    @Override
    protected PlanarImage createPlanarImage(int level) {
        if (level == 0) {
            return levelZeroImage;
        } else if (levelZeroImage instanceof DownscalableImage) {
            return PlanarImage.wrapRenderedImage(((DownscalableImage) levelZeroImage).downscale(level));
        } else {
            float scale = (float) pow2(-level);
            return ScaleDescriptor.create(levelZeroImage,
                                          scale, scale, 0.0f, 0.0f,
                                          interpolation, null).getRendering();
        }
    }

    @Override
    public Rectangle2D getBounds(int level) {
        checkLevel(level);
        return new Rectangle2D.Double(
                boundingBox.getX(), boundingBox.getY(),
                boundingBox.getWidth(), boundingBox.getHeight());
    }
}