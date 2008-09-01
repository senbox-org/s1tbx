package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LRImageFactory;
import com.bc.ceres.glevel.MRImage;

import javax.media.jai.RenderedImageAdapter;
import java.awt.image.RenderedImage;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class MRImageImpl extends RenderedImageAdapter implements MRImage {

    private final MRImageSupport mrImageSupport;

    public MRImageImpl(LRImageFactory lrImageFactory) {
        this(lrImageFactory.createLRImage(0), lrImageFactory);
    }

    public MRImageImpl(RenderedImage frImage, LRImageFactory lrImageFactory) {
        super(frImage);
        mrImageSupport = new MRImageSupport(frImage, lrImageFactory);
    }

    @Override
    public RenderedImage getLRImage(int level) {
        return mrImageSupport.getLRImage(level);
    }

    @Override
    public synchronized void dispose() {
        mrImageSupport.dispose();
        super.dispose();
    }
}
