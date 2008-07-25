package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.DownscalableImage;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

public class DefaultMultiLevelImage extends AbstractMultiLevelImage {

    private final PlanarImage sourceImage;
    private final Rectangle2D boundingBox;
    private final Interpolation interpolation;

    public DefaultMultiLevelImage(RenderedImage image,
                                  AffineTransform imageToModelTransform,
                                  int levelCount) {
        this(image,
             imageToModelTransform,
             levelCount,
             Interpolation.getInstance(Interpolation.INTERP_BICUBIC));
    }

    public DefaultMultiLevelImage(RenderedImage image,
                                  AffineTransform imageToModelTransform,
                                  int levelCount,
                                  Interpolation interpolation) {
        super(imageToModelTransform, levelCount);
        boundingBox = imageToModelTransform.createTransformedShape(new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight())).getBounds2D();
        sourceImage = PlanarImage.wrapRenderedImage(image);
        this.interpolation = interpolation;
    }

    @Override
    protected PlanarImage createPlanarImage(int level) {
        if (level == 0) {
            return sourceImage;
        } else if (sourceImage instanceof DownscalableImage) {
            final DownscalableImage downscalableImage = (DownscalableImage) sourceImage;
            return PlanarImage.wrapRenderedImage(downscalableImage.downscale(level));
        } else {
            float scale = (float) pow2(-level);
            return ScaleDescriptor.create(sourceImage,
                                          scale, scale, 0.0f, 0.0f,
                                          interpolation, null).getRendering();
        }
    }

    @Override
    public Rectangle2D getBoundingBox(int level) {
        checkLevel(level);
        return new Rectangle2D.Double(
                boundingBox.getX(), boundingBox.getY(),
                boundingBox.getWidth(), boundingBox.getHeight());
    }
}