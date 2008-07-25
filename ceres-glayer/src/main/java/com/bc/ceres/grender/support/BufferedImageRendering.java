package com.bc.ceres.grender.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;

import java.awt.*;
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
             new DefaultViewport(new Rectangle(0, 0, width, height)));
    }

    public BufferedImageRendering(BufferedImage image, Viewport viewport) {
        setViewport(viewport);
        setImage(image);
    }

    public Rectangle getBounds() {
        return new Rectangle(0, 0, image.getWidth(), image.getHeight());
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
