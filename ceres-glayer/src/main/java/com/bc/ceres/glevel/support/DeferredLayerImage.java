package com.bc.ceres.glevel.support;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.MultiLevelImage;

public class DeferredLayerImage extends AbstractLayerImage {

    private final LRImageFactory lrImageFactory;
    private MultiLevelImage mrImage;

    public DeferredLayerImage(AffineTransform imageToModelTransform, int levelCount, LRImageFactory lrImageFactory) {
        super(imageToModelTransform, levelCount);
        this.lrImageFactory = lrImageFactory;
    }

    public LRImageFactory getLRImageFactory() {
        return lrImageFactory;
    }

    public RenderedImage getLRImage(int level) {
        checkLevel(level);
        synchronized (this) {
            if (mrImage == null) {
                mrImage = new MultiResolutionImageImpl(lrImageFactory);
            }
            return mrImage.getLevelImage(level);
        }
    }

    @Override
    public synchronized void reset() {
        if (mrImage != null) {
            mrImage.dispose();
            mrImage = null;
        }
        super.reset();

    }

}