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

package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;

import javax.media.jai.ImageLayout;
import java.awt.Shape;
import java.awt.image.RenderedImage;

/**
 * Adapts a JAI {@link javax.media.jai.PlanarImage PlanarImage} to the
 * {@link com.bc.ceres.glevel.MultiLevelSource} interface.
 * The image data provided by this {@code PlanarImage} corresponds to the level zero image of the given
 * {@code MultiLevelSource}.
 *
 * @author Norman Fomferra
 */
public class DefaultMultiLevelImage extends MultiLevelImage {

    private final MultiLevelSource source;

    /**
     * Constructs a new multi-level image from the given source.
     *
     * @param source The multi-level image source.
     */
    public DefaultMultiLevelImage(MultiLevelSource source) {
        this(source,new ImageLayout(source.getImage(0)));
    }

    /**
     * Constructs a new multi-level image from the given source and the image layout.
     *
     * @param source The multi-level image source.
     * @param layout The image layout.
     */
    public DefaultMultiLevelImage(MultiLevelSource source, ImageLayout layout) {
        super(layout, null, null);
        this.source = source;
    }

    /**
     * @return The multi-level image source.
     */
    public final MultiLevelSource getSource() {
        return source;
    }

    /////////////////////////////////////////////////////////////////////////
    // MultiLevelImage interface

    @Override
    public final MultiLevelModel getModel() {
        return source.getModel();
    }

    @Override
    public final RenderedImage getImage(int level) {
        return source.getImage(level);
    }

    @Override
    public Shape getImageShape(int level) {
        return source.getImageShape(level);
    }

    @Override
    public void reset() {
        source.reset();
    }

    @Override
    public void dispose() {
        source.reset();
        super.dispose();
    }
}