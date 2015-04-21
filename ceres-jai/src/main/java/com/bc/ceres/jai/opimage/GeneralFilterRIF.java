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


import com.bc.ceres.jai.GeneralFilterFunction;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * @see javax.media.jai.operator.ConvolveDescriptor
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
    @Override
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
