package com.bc.ceres.jai.opimage;


import com.bc.ceres.jai.GeneralFilterFunction;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * @see com.sun.media.jai.opimage.ConvolveOpImage
 */
public class GeneralFilterRIF implements RenderedImageFactory {

    /**
     * Constructor.
     */
    public GeneralFilterRIF() {
    }

    /**
     * Create a new instance of ConvolveOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock The source image and the convolution kernel.
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {
        ImageLayout layout = renderHints != null ? (ImageLayout) renderHints.get(JAI.KEY_IMAGE_LAYOUT) : null;
        BorderExtender extender = renderHints != null ? (BorderExtender) renderHints.get(JAI.KEY_BORDER_EXTENDER) : null;
        GeneralFilterFunction filterFunction = (GeneralFilterFunction) paramBlock.getObjectParameter(0);
        return new GeneralFilterOpImage(paramBlock.getRenderedSource(0),
                                        extender,
                                        renderHints,
                                        layout,
                                        filterFunction);
    }
}
