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

import com.bc.ceres.glevel.MultiLevelRenderer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Rendering;

import java.awt.geom.AffineTransform;

public class DefaultMultiLevelRenderer implements MultiLevelRenderer {

    public DefaultMultiLevelRenderer() {
    }

    @Override
    public void renderImage(Rendering rendering, MultiLevelSource multiLevelSource, int level) {
        final AffineTransform i2m = multiLevelSource.getModel().getImageToModelTransform(level);
        final AffineTransform m2v = rendering.getViewport().getModelToViewTransform();
        i2m.preConcatenate(m2v);
        rendering.getGraphics().drawRenderedImage(multiLevelSource.getImage(level), i2m);
    }

    @Override
    public void reset() {
        // no state to dispose
    }
}