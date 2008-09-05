package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelSource;

import java.awt.image.RenderedImage;

/**
 * A {@code GenericMultiLevelSource} retrieves source level images
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class GenericMultiLevelSource extends AbstractMultiLevelSource {
    private final MultiLevelSource[] multiLevelSources;

    protected GenericMultiLevelSource(MultiLevelSource multiLevelSource) {
        this(new MultiLevelSource[]{multiLevelSource});
    }

    protected GenericMultiLevelSource(MultiLevelSource[] multiLevelSources) {
        super(multiLevelSources[0].getModel());
        this.multiLevelSources = multiLevelSources.clone();
    }

    public MultiLevelSource[] getMultiLevelSources() {
        return multiLevelSources.clone();
    }

    @Override
    protected RenderedImage createImage(int level) {
        RenderedImage[] sourceImages = new RenderedImage[multiLevelSources.length];
        for (int i = 0; i < multiLevelSources.length; i++) {
            sourceImages[i] = multiLevelSources[i].getImage(level);
        }
        return createImage(sourceImages, level);
    }

    protected abstract RenderedImage createImage(RenderedImage[] sourceImages, int level);
}
