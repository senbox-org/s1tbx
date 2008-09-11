package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;

import javax.media.jai.PlanarImage;
import java.awt.image.RenderedImage;

public abstract class AbstractMultiLevelSource implements MultiLevelSource {

    private final MultiLevelModel multiLevelModel;
    private final RenderedImage[] levelImages;

    public AbstractMultiLevelSource(MultiLevelModel multiLevelModel) {
        this.multiLevelModel = multiLevelModel;
        this.levelImages = new RenderedImage[multiLevelModel.getLevelCount()];
    }

    public MultiLevelModel getModel() {
        return multiLevelModel;
    }

    @Override
    public synchronized RenderedImage getImage(int level) {
        checkLevel(level);
        RenderedImage levelImage = levelImages[level];
        if (levelImage == null) {
            levelImage = createImage(level);
            levelImages[level] = levelImage;
        }
        return levelImage;
    }

    protected abstract RenderedImage createImage(int level);


    /**
     * <p>The {@code CachingLevelImageSource} defines this method to remove the cached
     * level images and also dispose any {@link javax.media.jai.PlanarImage PlanarImage}s
     * among them.</p>
     * <p/>
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
        if (level < 0 || level >= multiLevelModel.getLevelCount()) {
            throw new IllegalArgumentException("level=" +level);
        }
    }
}