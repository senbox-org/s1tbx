package com.bc.ceres.glayer;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A rendering which uses a buffered image as drawing surface.
 */
public class BufferedImageRendering implements Rendering {
    private BufferedImage image;
    private Viewport viewport;
    private Graphics2D graphics;

    public BufferedImageRendering(int width, int height) {
        this(new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR));
    }

    public BufferedImageRendering(BufferedImage image) {
        this.image = image;
        this.viewport = new Viewport();
    }

    public BufferedImage getImage() {
        return image;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public synchronized Graphics2D getGraphics() {
        if (graphics == null) {
            graphics = image.createGraphics();
        }
        return graphics;
    }

    public Rectangle getBounds() {
        return new Rectangle(image.getMinX(),
                             image.getMinY(),
                             image.getWidth(),
                             image.getHeight());
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (graphics != null) {
            graphics.dispose();
        }
    }
}
