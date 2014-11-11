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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * A rendering which uses a buffered image as drawing surface.
 */
public class BufferedImageRendering implements Rendering {
    private BufferedImage image;
    private Graphics2D graphics;
    private Viewport viewport;

    public BufferedImageRendering(int width, int height) {
        this(new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR),
             new DefaultViewport(new Rectangle(0, 0, width, height), true));
    }

    public BufferedImageRendering(BufferedImage image) {
        this(image, new DefaultViewport(new Rectangle(0, 0, image.getWidth(), image.getHeight()), true));
    }

    public BufferedImageRendering(BufferedImage image, Viewport viewport) {
        Assert.notNull(image, "image");
        Assert.notNull(viewport, "viewport");
        this.image = image;
        this.viewport = viewport;
        updateViewportViewBounds();
    }

    @Override
    public synchronized Graphics2D getGraphics() {
        if (graphics == null) {
            graphics = image.createGraphics();
        }
        return graphics;
    }

    @Override
    public synchronized Viewport getViewport() {
        return viewport;
    }

    public synchronized void setViewport(Viewport viewport) {
        Assert.notNull(viewport, "viewport");
        this.viewport = viewport;
        updateViewportViewBounds();
    }

    public synchronized BufferedImage getImage() {
        return image;
    }

    public synchronized void setImage(BufferedImage image) {
        Assert.notNull(image, "image");
        disposeGraphics();
        this.image = image;
        updateViewportViewBounds();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        disposeGraphics();
    }

    private void disposeGraphics() {
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
    }

    private void updateViewportViewBounds() {
        this.viewport.setViewBounds(new Rectangle(this.image.getMinX(),
                                                  this.image.getMinY(),
                                                  this.image.getWidth(),
                                                  this.image.getHeight()));
    }
}
