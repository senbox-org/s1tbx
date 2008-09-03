package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MRImage;

import javax.media.jai.Interpolation;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class DefaultMultiLevelImage extends AbstractMultiLevelImage {

    private final MRImage levelZeroImage;

    public DefaultMultiLevelImage(MRImage mrImage, AffineTransform imageToModelTransform, int levelCount) {
        super(imageToModelTransform, levelCount);
        setModelBounds(getModelBounds(imageToModelTransform, mrImage));
        this.levelZeroImage = mrImage;
    }

    public DefaultMultiLevelImage(RenderedImage levelZeroImage,
            AffineTransform imageToModelTransform, int levelCount,
            Interpolation interpolation) {
        super(imageToModelTransform, levelCount);
        setModelBounds(getModelBounds(imageToModelTransform, levelZeroImage));
        this.levelZeroImage = new MRImageImpl(new LRImageFactoryImpl(levelZeroImage, interpolation));
    }

    public RenderedImage getLRImage(int level) {
        return levelZeroImage.getLRImage(level);
    }

}