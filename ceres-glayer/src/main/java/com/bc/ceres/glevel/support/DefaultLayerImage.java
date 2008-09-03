package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelImage;

import javax.media.jai.Interpolation;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class DefaultLayerImage extends AbstractLayerImage {

    private final MultiLevelImage levelZeroImage;

    public DefaultLayerImage(MultiLevelImage mrImage, AffineTransform imageToModelTransform, int levelCount) {
        super(imageToModelTransform, levelCount);
        setModelBounds(getModelBounds(imageToModelTransform, mrImage));
        this.levelZeroImage = mrImage;
    }

    public DefaultLayerImage(RenderedImage levelZeroImage,
            AffineTransform imageToModelTransform, int levelCount,
            Interpolation interpolation) {
        super(imageToModelTransform, levelCount);
        setModelBounds(getModelBounds(imageToModelTransform, levelZeroImage));
        this.levelZeroImage = new MultiResolutionImageImpl(new LRImageFactoryImpl(levelZeroImage, interpolation));
    }

    public RenderedImage getLRImage(int level) {
        return levelZeroImage.getLevelImage(level);
    }

}