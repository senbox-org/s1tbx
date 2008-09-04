package com.bc.ceres.glevel.support;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.image.RenderedImage;

public class DefaultLevelImageSource extends AbstractLevelImageSource {

    private final RenderedImage level0Image;
    private final Interpolation interpolation;

    public DefaultLevelImageSource(RenderedImage level0Image) {
        this(level0Image, 1, null);
    }

    public DefaultLevelImageSource(RenderedImage level0Image, int levelCount, Interpolation interpolation) {
        super(levelCount);
        this.level0Image = level0Image;
        this.interpolation = interpolation;
    }

    /**
     * Returns the level-0 image if {@code level} equals zero, otherwise calls {@code super.getLevelImage(level)}.
     * @param level The level.
     * @return The image.
     */
    @Override
    public synchronized RenderedImage getLevelImage(int level) {
        if (level == 0) {
            return level0Image;
        }
        return super.getLevelImage(level);
    }

    /**
     * Returns the level-0 image if {@code level} equals zero, otherwise creates a scaled version of it.
     * @param level The level.
     * @return The image.
     */
    @Override
    public RenderedImage createLevelImage(int level) {
        if (level == 0) {
            return level0Image;
        }
        final float scale = (float) computeScale(level);
        return ScaleDescriptor.create(level0Image, scale, scale, 0.0f, 0.0f, interpolation, null);
    }

}