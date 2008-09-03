package com.bc.ceres.glevel.support;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import com.bc.ceres.glevel.LevelImageFactory;
import com.bc.ceres.glevel.MultiLevelImage;

public class DeferredLayerImage extends AbstractLayerImage {

    private final LevelImageFactory levelImageFactory;
    private MultiLevelImage mrImage;

    public DeferredLayerImage(AffineTransform imageToModelTransform, int levelCount, LevelImageFactory levelImageFactory) {
        super(imageToModelTransform, levelCount);
        this.levelImageFactory = levelImageFactory;
    }

    public LevelImageFactory getLevelImageFactory() {
        return levelImageFactory;
    }

    public RenderedImage getLRImage(int level) {
        checkLevel(level);
        synchronized (this) {
            if (mrImage == null) {
                mrImage = new MultiLevelImageImpl(levelImageFactory);
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