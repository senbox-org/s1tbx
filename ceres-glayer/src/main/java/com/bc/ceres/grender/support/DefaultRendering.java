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

package com.bc.ceres.grender.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;

import java.awt.*;

/**
 * A default implementation of the {@link com.bc.ceres.grender.Rendering} interface.
 */
public class DefaultRendering implements Rendering {
    private Graphics2D graphics;
    private Viewport viewport;

    public DefaultRendering(Viewport viewport) {
        setViewport(viewport);
    }

    public DefaultRendering(Viewport viewport, Graphics2D graphics) {
        this.viewport = viewport;
        this.graphics = graphics;
    }

    @Deprecated
    public DefaultRendering(Graphics2D graphics, Viewport viewport) {
        setGraphics(graphics);
        setViewport(viewport);
    }

    @Override
    public Graphics2D getGraphics() {
        return graphics;
    }

    public void setGraphics(Graphics2D graphics) {
        Assert.notNull(graphics, "graphics");
        this.graphics = graphics;
    }

    @Override
    public Viewport getViewport() {
        return viewport;
    }

    public void setViewport(Viewport viewport) {
        Assert.notNull(viewport, "viewport");
        this.viewport = viewport;
    }
}