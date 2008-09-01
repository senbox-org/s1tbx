package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MRImage;

import javax.media.jai.Interpolation;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class DefaultMultiLevelImage extends AbstractMultiLevelImage {

    private final MRImage levelZeroImage;


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
        setModelBounds(getModelBounds(imageToModelTransform, levelZeroImage));
        this.levelZeroImage = levelZeroImage instanceof MRImage ? (MRImage) levelZeroImage : new MRImageImpl(new LRImageFactoryImpl(levelZeroImage, interpolation));
    }

    public RenderedImage getLRImage(int level) {
        return levelZeroImage.getLRImage(level);
    }

}