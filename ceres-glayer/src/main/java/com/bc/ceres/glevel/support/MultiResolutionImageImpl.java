package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImageFactory;
import com.bc.ceres.glevel.MultiLevelImage;

import javax.media.jai.RenderedImageAdapter;
import java.awt.image.RenderedImage;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class MultiResolutionImageImpl extends RenderedImageAdapter implements MultiLevelImage {

    private final MultiLevelImageSupport multiLevelImageSupport;

    public MultiResolutionImageImpl(LevelImageFactory levelImageFactory) {
        this(levelImageFactory.createLRImage(0), levelImageFactory);
    }

    public MultiResolutionImageImpl(RenderedImage frImage, LevelImageFactory levelImageFactory) {
        super(frImage);
        multiLevelImageSupport = new MultiLevelImageSupport(frImage, levelImageFactory);
    }

    @Override
    public RenderedImage getLevelImage(int level) {
        return multiLevelImageSupport.getLRImage(level);
    }

    @Override
    public synchronized void dispose() {
        multiLevelImageSupport.dispose();
        super.dispose();
    }
}
