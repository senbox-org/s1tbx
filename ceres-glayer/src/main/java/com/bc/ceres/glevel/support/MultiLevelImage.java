package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImageSource;

import javax.media.jai.RenderedImageAdapter;
import java.awt.image.RenderedImage;

/**
 * Adapts a JAI {@link javax.media.jai.PlanarImage PlanarImage} to the {@link LevelImageSource} interface.
 * The image data provided by this {@code PlanarImage} corresponds to the level zero image of the given
 * {@code LevelImageSource}.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class MultiLevelImage extends RenderedImageAdapter implements LevelImageSource {

    private final LevelImageSource levelImageSource;


    public MultiLevelImage(LevelImageSource levelImageSource) {
        super(levelImageSource.getLevelImage(0));
        this.levelImageSource = levelImageSource;
    }

    public LevelImageSource getLevelImageSource() {
        return levelImageSource;
    }

    @Override
    public int getLevelCount() {
        return levelImageSource.getLevelCount();
    }

    @Override
    public RenderedImage getLevelImage(int level) {
        return levelImageSource.getLevelImage(level);
    }

    @Override
    public int computeLevel(double scale) {
        return levelImageSource.computeLevel(scale);
    }

    @Override
    public double computeScale(int level) {
        return levelImageSource.computeScale(level);
    }

    @Override
    public void reset() {
        levelImageSource.reset();
    }

    @Override
    public synchronized void dispose() {
        reset();
        super.dispose();
    }
}