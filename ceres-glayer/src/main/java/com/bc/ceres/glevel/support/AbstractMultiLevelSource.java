package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.RenderedImage;

/**
 * An abstract base class for {@link MultiLevelSource} implementations.
 * Level images are cached unless {@link #reset()} is called.
 * Subclasses are asked tzo implement {@link #createImage(int)}.
 */
public abstract class AbstractMultiLevelSource implements MultiLevelSource {

    private final MultiLevelModel multiLevelModel;
    private final RenderedImage[] levelImages;

    protected AbstractMultiLevelSource(MultiLevelModel multiLevelModel) {
        this.multiLevelModel = multiLevelModel;
        this.levelImages = new RenderedImage[multiLevelModel.getLevelCount()];
    }

    @Override
    public MultiLevelModel getModel() {
        return multiLevelModel;
    }

    /**
     * Gets the {@code RenderedImage} at the given resolution level. Unless {@link #reset()} is called,
     * the method will always return the same image instance at the same resolution level.
     * If a level image is requested for the first time, the method calls
     * {@link #createImage(int)} in order to retrieve the actual image instance.
     *
     * @param level The resolution level.
     *
     * @return The {@code RenderedImage} at the given resolution level.
     */
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

    @Override
    public Shape getImageShape(int level) {
        return null;
    }

    /**
     * Called by {@link #getImage(int)} if a level image is requested for the first time.
     *
     * @param level The resolution level.
     *
     * @return An instance of a {@code RenderedImage} for the given resolution level.
     */
    protected abstract RenderedImage createImage(int level);


    /**
     * Removes all cached level images and also disposes
     * any {@link javax.media.jai.PlanarImage PlanarImage}s among them.</p>
     * <p/>
     * <p>Overrides should always call {@code super.reset()}.<p/>
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

    /**
     * Utility method which checks if a given level is valid.
     *
     * @param level The resolution level.
     *
     * @throws IllegalArgumentException if {@code level &lt; 0 || level &gt;= getModel().getLevelCount()}
     */
    protected synchronized void checkLevel(int level) {
        if (level < 0 || level >= getModel().getLevelCount()) {
            throw new IllegalArgumentException("level=" + level);
        }
    }
}