package com.bc.ceres.swing.figure.support;

import com.bc.ceres.core.Assert;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Symbol;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * A symbol that uses an icon image.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
public class ImageSymbol implements Symbol {

    private static final Color BGCOLOR = new Color(0, 0, 0, 0);
    private final String resourcePath;
    private final BufferedImage image;
    private final double refX;
    private final double refY;

    public static ImageSymbol createIcon(String imageResourcePath) {
        return createIcon(imageResourcePath, ImageSymbol.class);
    }

    public static ImageSymbol createIcon(String imageResourcePath, Class callerClass) {
        BufferedImage image = loadBufferedImage(imageResourcePath, callerClass);
        return new ImageSymbol(imageResourcePath, image, 0.5 * image.getWidth(), 0.5 * image.getHeight());
    }

    public static ImageSymbol createIcon(String imageResourcePath, double refX, double refY) {
        return createIcon(imageResourcePath, refX, refY, ImageSymbol.class);
    }

    public static ImageSymbol createIcon(String imageResourcePath, double refX, double refY, Class callerClass) {
        BufferedImage image = loadBufferedImage(imageResourcePath, callerClass);
        return new ImageSymbol(imageResourcePath, image, refX, refY);
    }

    private ImageSymbol( String resourcePath, BufferedImage image, double refX, double refY) {
        this.resourcePath = resourcePath;
        this.image = image;
        this.refX = refX;
        this.refY = refY;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public BufferedImage getImage() {
        return image;
    }

    /**
     * @return The X-coordinate of the reference point.
     */
    public double getRefX() {
        return refX;
    }

    /**
     * @return The Y-coordinate of the reference point.
     */
    public double getRefY() {
        return refY;
    }

    @Override
    public void draw(Rendering rendering, FigureStyle style) {
        try {
            rendering.getGraphics().translate(-refX, -refY);
            // improvement: we could check if we have to filter the image, e.g. to display in different colours
            rendering.getGraphics().drawRenderedImage(image, null);
        } finally {
            rendering.getGraphics().translate(+refX, +refY);
        }
    }

    @Override
    public boolean isHitBy(double x, double y) {
        int ix = (int) Math.round(x + refX);
        int iy = (int) Math.round(y + refY);
        return ix >= 0
                && ix < image.getWidth()
                && iy >= 0
                && iy < image.getHeight()
                && (image.getRGB(ix, iy) & 0xff000000) != 0;
    }

    @Override
    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(-refX, -refY, image.getWidth(), image.getHeight());
    }

    private static BufferedImage loadBufferedImage(String imageResourcePath, Class callerClass) {
        URL resource = callerClass.getResource(imageResourcePath);
        Assert.argument(resource != null, "imageResourcePath: resource not found: " + imageResourcePath);
        ImageIcon imageIcon = new ImageIcon(resource);
        BufferedImage image = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(imageIcon.getImage(), 0, 0, BGCOLOR, imageIcon.getImageObserver());
        graphics.dispose();
        return image;
    }
}
