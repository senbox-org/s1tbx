package com.bc.ceres.glevel.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.LRImageFactory;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class DeferredMultiLevelImage extends AbstractMultiLevelImage {

    private LRImageFactory lrImageFactory;
    private MRImageImpl mrImage;

    public DeferredMultiLevelImage(AffineTransform imageToModelTransform, int levelCount) {
        super(imageToModelTransform, levelCount);
    }

    public DeferredMultiLevelImage(AffineTransform imageToModelTransform, int levelCount, LRImageFactory lrImageFactory) {
        super(imageToModelTransform, levelCount);
        this.lrImageFactory = lrImageFactory;
    }

    public LRImageFactory getLRImageFactory() {
        return lrImageFactory;
    }

    public void setLRImageFactory(LRImageFactory lrImageFactory) {
        reset();
        this.lrImageFactory = lrImageFactory;
    }

    public RenderedImage getLRImage(int level) {
        checkLevel(level);
        synchronized (this) {
            if (mrImage == null) {
                Assert.state(lrImageFactory != null, "lrImageFactory != null");
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