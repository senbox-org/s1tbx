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


import javax.media.jai.ImageLayout;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * The RIF for the "paint" operation.
 */
public class PaintRIF implements RenderedImageFactory {

    /**
     * Create a new instance of PaintOpImage in the rendered layer.
     * This method satisfies the implementation of RIF.
     *
     * @param paramBlock The source images and paint color.
     */
    @Override
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {
        RenderedImage source0 = paramBlock.getRenderedSource(0);
        RenderedImage source1 = paramBlock.getRenderedSource(1);
        Color paintColor = (Color) paramBlock.getObjectParameter(0);
        Boolean alphaIsFirst = (Boolean) paramBlock.getObjectParameter(1);
        ImageLayout layout = new ImageLayout(source0);
        return new PaintOpImage(source0,
                                source1,
                                renderHints,
                                layout,
                                paintColor,
                                alphaIsFirst);
    }
}
