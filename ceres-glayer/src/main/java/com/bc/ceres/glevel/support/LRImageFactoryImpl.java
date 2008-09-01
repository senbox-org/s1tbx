package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MRImage;
import com.bc.ceres.glevel.LRImageFactory;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.image.RenderedImage;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class LRImageFactoryImpl implements LRImageFactory {
    private final RenderedImage frSourceImage;

    public LRImageFactoryImpl(RenderedImage frSourceImage) {
        this.frSourceImage = frSourceImage;
    }

    public RenderedImage getFRSourceImage() {
        return frSourceImage;
    }

    @Override
    public final RenderedImage createLRImage(int level) {
        final RenderedImage lrSource;
        if (level > 0) {
            if (getFRSourceImage() instanceof MRImage) {
                lrSource = ((MRImage) getFRSourceImage()).getLRImage(level);
            } else {
                lrSource = createLRSourceImage(convertLevelToScale(level));
            }
        } else {
            lrSource = getFRSourceImage();
        }
        return createLRTargetImage(lrSource);
    }

    protected double convertLevelToScale(int level) {
        return Math.pow(2, -level);
    }

    protected RenderedImage createLRSourceImage(double scale) {
        return ScaleDescriptor.create(getFRSourceImage(),
                                      (float)scale, (float)scale,
                                      0.0f, 0.0f,
                                      Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                      null);
    }

    protected RenderedImage createLRTargetImage(RenderedImage lrSource) {
        return lrSource;
    }
}