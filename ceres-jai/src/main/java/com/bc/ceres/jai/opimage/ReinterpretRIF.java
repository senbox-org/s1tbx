package com.bc.ceres.jai.opimage;

import com.bc.ceres.jai.operator.InterpretationType;
import com.bc.ceres.jai.operator.ScalingType;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

public class ReinterpretRIF implements RenderedImageFactory {

    /**
     * Create a new instance of ReinterpretOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock The source image and the convolution kernel.
     * @param hints      The rendering hints.
     *
     * @return the rendered image created.
     */
    @Override
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints hints) {
        return ReinterpretOpImage.create(paramBlock.getRenderedSource(0), paramBlock.getDoubleParameter(0),
                                         paramBlock.getDoubleParameter(1),
                                         (ScalingType) paramBlock.getObjectParameter(2),
                                         (InterpretationType) paramBlock.getObjectParameter(3), hints
        );
    }
}
