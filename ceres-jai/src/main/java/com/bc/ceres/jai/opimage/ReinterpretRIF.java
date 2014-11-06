/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.jai.opimage;

import com.bc.ceres.jai.operator.InterpretationType;
import com.bc.ceres.jai.operator.ScalingType;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import static com.bc.ceres.jai.operator.ReinterpretDescriptor.AWT;
import static com.bc.ceres.jai.operator.ReinterpretDescriptor.LINEAR;

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
        final RenderedImage source = paramBlock.getRenderedSource(0);
        final double factor = paramBlock.getDoubleParameter(0);
        final double offset = paramBlock.getDoubleParameter(1);
        final ScalingType scalingType = (ScalingType) paramBlock.getObjectParameter(2);
        final InterpretationType interpretationType = (InterpretationType) paramBlock.getObjectParameter(3);
        if (factor == 1.0 && offset == 0.0 && scalingType == LINEAR && interpretationType == AWT) {
            return source;
        }
        return ReinterpretOpImage.create(source, factor, offset, scalingType, interpretationType, hints);
    }
}
