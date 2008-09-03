package com.bc.ceres.glevel.support;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.MRImage;

public class DeferredMultiLevelImage extends AbstractMultiLevelImage {

    private final LRImageFactory lrImageFactory;
    private MRImage mrImage;

    public DeferredMultiLevelImage(AffineTransform imageToModelTransform, int levelCount, LRImageFactory lrImageFactory) {
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
                mrImage = new MRImageImpl(lrImageFactory);
            }
            return mrImage.getLRImage(level);
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