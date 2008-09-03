package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LRImageFactory;
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

    private final MRImageSupport mrImageSupport;

    public MultiResolutionImageImpl(LRImageFactory lrImageFactory) {
        this(lrImageFactory.createLRImage(0), lrImageFactory);
    }

    public MultiResolutionImageImpl(RenderedImage frImage, LRImageFactory lrImageFactory) {
        super(frImage);
        mrImageSupport = new MRImageSupport(frImage, lrImageFactory);
    }

    @Override
    public RenderedImage getLevelImage(int level) {
        return mrImageSupport.getLRImage(level);
    }

    @Override
    public synchronized void dispose() {
        mrImageSupport.dispose();
        super.dispose();
    }
}
