package org.esa.beam.framework.ui.product;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.FigureStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A point symbol that is represented by an image.
 *
 * @author Norman Fomferra
 * @since Ceres 0.13
 */
public class ImageSymbol implements PointSymbol {

    private static final Color BGCOLOR = new Color(0, 0, 0, 0);

    private final BufferedImage image;
    private final double refX;
    private final double refY;

    public static ImageSymbol createIcon(Class callerClass, String imageResourcePath) {
        BufferedImage image = loadImageResource(callerClass, imageResourcePath);
        return new ImageSymbol(image,  0.5 * image.getWidth(), 0.5 * image.getHeight());
    }


    public static ImageSymbol createIcon(Class callerClass, String imageResourcePath, double refX, double refY) {
        BufferedImage image = loadImageResource(callerClass, imageResourcePath);
        return new ImageSymbol(image, refX, refY);
    }

    private static BufferedImage loadImageResource(Class callerClass, String imageResourcePath) {
        ImageIcon imageIcon = new ImageIcon(callerClass.getResource(imageResourcePath));
        BufferedImage image = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(imageIcon.getImage(), 0, 0, BGCOLOR, imageIcon.getImageObserver());
        return image;
    }

    public ImageSymbol(BufferedImage image) {
        this(image, 0.5 * image.getWidth(), 0.5 * image.getHeight());
    }

    public ImageSymbol(BufferedImage image, double refX, double refY) {
        this.image = image;
        this.refX = refX;
        this.refY = refY;
    }

    @Override
    public double getRefX() {
        return refX;
    }

    @Override
    public double getRefY() {
        return refY;
    }

    @Override
    public void draw(Rendering rendering, FigureStyle style) {
        rendering.getGraphics().drawRenderedImage(image, null);
    }

    @Override
    public boolean containsPoint(double x, double y) {
        int ix = (int) Math.round(refX + x);
        int iy = (int) Math.round(refY + y);
        return ix >= 0
                && ix < image.getWidth()
                && iy >= 0
                && iy < image.getHeight()
                && (image.getRGB(ix, iy) & 0xff000000) != 0;
    }
}
