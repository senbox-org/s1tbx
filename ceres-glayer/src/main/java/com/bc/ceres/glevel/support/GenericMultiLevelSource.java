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

import com.bc.ceres.glevel.MultiLevelSource;

import java.awt.image.RenderedImage;

/**
 * A {@code GenericMultiLevelSource} is a {@link MultiLevelSource} computing its
 * images at a given resolution level from a number of source images of the same level.
 * <p>Subclasses will have to to implement {@link #createImage(java.awt.image.RenderedImage[], int)}.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class GenericMultiLevelSource extends AbstractMultiLevelSource {
    private final MultiLevelSource[] multiLevelSources;

    protected GenericMultiLevelSource(MultiLevelSource multiLevelSource) {
        this(new MultiLevelSource[]{multiLevelSource});
    }

    protected GenericMultiLevelSource(MultiLevelSource[] multiLevelSources) {
        super(multiLevelSources[0].getModel());
        this.multiLevelSources = multiLevelSources.clone();
    }

    public MultiLevelSource[] getMultiLevelSources() {
        return multiLevelSources.clone();
    }

    @Override
    protected RenderedImage createImage(int level) {
        RenderedImage[] sourceImages = new RenderedImage[multiLevelSources.length];
        for (int i = 0; i < multiLevelSources.length; i++) {
            sourceImages[i] = multiLevelSources[i].getImage(level);
        }
        return createImage(sourceImages, level);
    }

    protected abstract RenderedImage createImage(RenderedImage[] sourceImages, int level);
}
