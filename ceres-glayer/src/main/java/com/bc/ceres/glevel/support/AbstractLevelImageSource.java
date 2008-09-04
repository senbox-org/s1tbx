package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImageSource;

import javax.media.jai.PlanarImage;
import java.awt.image.RenderedImage;

public abstract class AbstractLevelImageSource implements LevelImageSource {

    private final int levelCount;
    private final RenderedImage[] levelImages;

    public AbstractLevelImageSource(int levelCount) {
        this.levelCount = levelCount;
        this.levelImages = new RenderedImage[levelCount];
    }

    @Override
    public int getLevelCount() {
        return levelCount;
    }

    @Override
    public int computeLevel(double scale) {
        int level = (int) Math.round(log2(scale));
        if (level < 0) {
            level = 0;
        } else if (level >= levelCount) {
            level = levelCount - 1;
        }
        return level;
    }

    @Override
    public double computeScale(int level) {
        checkLevel(level);
        return pow2(level);
    }

    @Override
    public synchronized RenderedImage getLevelImage(int level) {
        checkLevel(level);
        RenderedImage levelImage = levelImages[level];
        if (levelImage == null) {
            levelImage = createLevelImage(level);
            levelImages[level] = levelImage;
        }
        return levelImage;
    }

    protected abstract RenderedImage createLevelImage(int level);


    /**
     * <p>The {@code CachingLevelImageSource} defines this method to remove the cached
     * level images and also dispose any {@link javax.media.jai.PlanarImage PlanarImage}s
     * among them.</p>
     *
     * <p>Subclasses should call
     * <code>super.dispose()</code> in their <code>cleanUp</code>
     * methods, if any.<p/>
     */
    @Override
    public synchronized void reset() {
        for (int level = 0; level < levelImages.length; level++) {
            RenderedImage levelImage = levelImages[level];
            if (levelImage instanceof PlanarImage) {
                PlanarImage planarImage = (PlanarImage) levelImage;
                planarImage.dispose();
            }
            levelImages[level] = null;
        }
    }

    protected void checkLevel(int level) {
        if (level < 0 || level >= levelCount) {
            throw new IllegalArgumentException("level");
        }
    }

    protected static double pow2(double v) {
        return Math.pow(2.0, v);
    }

    protected static double log2(double v) {
        return Math.log(v) / Math.log(2.0);
    }
}