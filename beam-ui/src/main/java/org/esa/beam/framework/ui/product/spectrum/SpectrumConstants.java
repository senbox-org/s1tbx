package org.esa.beam.framework.ui.product.spectrum;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class SpectrumConstants {

    private static final int starXPoints[] = {0, 1, 6, 2, 3, 0, -3, -2, -6, -1};
    private static final int starYPoints[] = {-6, -2, -2, 0, 5, 2, 5, 0, -2, -2};
    private static final int xXPoints[] = {-5, -4, 0, 4, 5, 1, 5, 4, 0, -4, -5, -1};
    private static final int xYPoints[] = {-4, -5, -1, -5, -4, 0, 4, 5, 1, 5, 4, 0};
    private static final int crossXPoints[] = {-5, -1, -1, 1, 1, 5, 5, 1, 1, -1, -1, -5};
    private static final int crossYPoints[] = {-1, -1, -5, -5, -1, -1, 1, 1, 5, 5, 1, 1};
    public static final Shape[] shapes = new Shape[]{
            new Polygon(new int[]{-4, 0, 4, 0}, new int[]{0, -4, 0, 4}, 4),
            new Polygon(new int[]{-4, 0, 4}, new int[]{4, -4, 4}, 3),
            new Polygon(new int[]{-4, 0, 4}, new int[]{-4, 4, -4}, 3),
            new Polygon(starXPoints, starYPoints, starXPoints.length),
            new Polygon(xXPoints, xYPoints, xXPoints.length),
            new Polygon(crossXPoints, crossYPoints, crossXPoints.length),
            new Ellipse2D.Double(-3, -3, 6, 6),
            new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0),
            new Rectangle2D.Double(-2.0, -5.0, 4.0, 10.0),
            new Rectangle2D.Double(-5.0, -2.0, 10.0, 4.0)
    };
    public static final ImageIcon[] shapeIcons = convertShapesToIcons();
    public static final Stroke[] strokes = new Stroke[]{
            new BasicStroke(),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{1.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 1.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{20.0f, 5.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{1.0f, 5.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f, 1.0f, 10.0f}, 0.0f)
    };
    public static final ImageIcon[] strokeIcons = convertStrokesToIcons();

    private static ImageIcon[] convertShapesToIcons() {
        ImageIcon[] icons = new ImageIcon[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            icons[i] = convertShapeToIcon(shapes[i]);
        }
        return icons;
    }

    private static ImageIcon convertShapeToIcon(Shape seriesShape) {
        Rectangle rectangle = seriesShape.getBounds();
        BufferedImage image = new BufferedImage((int) (rectangle.getWidth() - rectangle.getX()),
                                                (int) (rectangle.getHeight() - rectangle.getY()),
                                                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.translate(-rectangle.x, -rectangle.y);
        graphics.setColor(Color.BLACK);
        graphics.draw(seriesShape);
        graphics.dispose();
        return new ImageIcon(image);
    }

    private static ImageIcon[] convertStrokesToIcons() {
        ImageIcon[] icons = new ImageIcon[strokes.length];
        for (int i = 0; i < strokes.length; i++) {
            icons[i] = convertStrokeToIcon(strokes[i]);
        }
        return icons;
    }

    private static ImageIcon convertStrokeToIcon(Stroke stroke) {
        Shape strokeShape = new Line2D.Double(-40, 0, 40, 0);
        final Rectangle rectangle = strokeShape.getBounds();
        BufferedImage image = new BufferedImage((int) (rectangle.getWidth() - rectangle.getX()),
                                                1,
                                                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.translate(-rectangle.x, -rectangle.y);
        graphics.setColor(Color.BLACK);
        graphics.setStroke(stroke);
        graphics.draw(strokeShape);
        graphics.dispose();
        return new ImageIcon(image);
    }


}
