package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelModel;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public class DefaultMultiLevelSource extends AbstractMultiLevelSource {

    public final static DefaultMultiLevelSource NULL = new DefaultMultiLevelSource(new DefaultMultiLevelModel(1, new AffineTransform(), new Rectangle()),
                                                                                   new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY));

    private final RenderedImage level0Image;
    private final Interpolation interpolation;

    public DefaultMultiLevelSource(MultiLevelModel multiLevelModel, RenderedImage level0Image) {
        this(multiLevelModel, level0Image, Interpolation.getInstance(Interpolation.INTERP_BICUBIC));
    }

    public DefaultMultiLevelSource(MultiLevelModel multiLevelModel, RenderedImage level0Image, Interpolation interpolation) {
        super(multiLevelModel);
        this.level0Image = level0Image;
        this.interpolation = interpolation;
    }


    /**
     * Returns the level-0 image if {@code level} equals zero, otherwise calls {@code super.getLevelImage(level)}.
     *
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
     *
     * @param level The level.
     * @return The image.
     */
    @Override
    public RenderedImage createLevelImage(int level) {
        if (level == 0) {
            return level0Image;
        }
        final float scale = (float) (1.0 / getModel().getScale(level));
        return ScaleDescriptor.create(level0Image, scale, scale, 0.0f, 0.0f, interpolation, null);
    }


}