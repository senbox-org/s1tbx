package com.bc.ceres.glayer;

import com.bc.ceres.core.Assert;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A rendering which uses a buffered image as drawing surface.
 */
public class BufferedImageRendering implements Rendering {
    private Viewport viewport;
    private BufferedImage image;
    private Graphics2D graphics;

    public BufferedImageRendering(int width, int height) {
        this(new Viewport(), new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR));
    }

    public BufferedImageRendering(Viewport viewport, BufferedImage image) {
        setViewport(viewport);
        setImage(image);
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void setViewport(Viewport viewport) {
        Assert.notNull(viewport, "viewport");
        this.viewport = viewport;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        Assert.notNull(image, "image");
        disposeGraphics();
        this.image = image;
    }

    public synchronized Graphics2D getGraphics() {
        if (graphics == null) {
            graphics = image.createGraphics();
        }
        return graphics;
    }

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
}
