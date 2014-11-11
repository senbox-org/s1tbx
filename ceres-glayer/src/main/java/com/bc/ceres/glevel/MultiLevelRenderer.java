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

package com.bc.ceres.glevel;

import com.bc.ceres.grender.Rendering;

/**
 * An {@code MultiLevelRenderer} is used to render images provided
 * by a {@link MultiLevelSource}s at a certain resolution level.
 */
public interface MultiLevelRenderer {
    /**
     * Renders the image provided by the given {@link MultiLevelSource} at
     * the specified resolution level onto
     * a {@link com.bc.ceres.grender.Rendering Rendering}.
     *
     * @param rendering        The rendering.
     * @param multiLevelSource The multi-resolution image source.
     * @param level            The resolution level.
     */
    void renderImage(Rendering rendering, MultiLevelSource multiLevelSource, int level);


    /**
     * Releases any allocated resources hold by this {@code MultiLevelRenderer},
     * e.g. releases images or image tiles cached over multiple calls to {@link #renderImage}.
     */
    void reset();
}