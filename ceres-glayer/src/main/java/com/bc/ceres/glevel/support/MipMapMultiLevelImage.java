package com.bc.ceres.glevel.support;

import javax.media.jai.ImageMIPMap;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

public class MipMapMultiLevelImage extends AbstractMultiLevelImage {

    private final ImageMIPMap imageMIPMap;
    private final Rectangle2D boundingBox;

    public MipMapMultiLevelImage(RenderedImage image,
                                 AffineTransform imageToModelTransform,
                                 int levelCount,
                                 Interpolation interpolation) {
        super(imageToModelTransform, levelCount);
        final RenderedOp downSampler = ScaleDescriptor.create(image, 0.5f, 0.5f, 0.0f, 0.0f, interpolation, null);
        downSampler.removeSources();
        this.imageMIPMap = new ImageMIPMap(image, downSampler);
        this.boundingBox = imageToModelTransform.createTransformedShape(new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight())).getBounds2D();
    }

    @Override
    protected PlanarImage createPlanarImage(int level) {
        return PlanarImage.wrapRenderedImage(imageMIPMap.getImage(level));
    }

    @Override
    public Rectangle2D getBoundingBox(int level) {
        checkLevel(level);
        return new Rectangle2D.Double(boundingBox.getX(), boundingBox.getY(),
                                      boundingBox.getWidth(), boundingBox.getHeight());
    }
}
