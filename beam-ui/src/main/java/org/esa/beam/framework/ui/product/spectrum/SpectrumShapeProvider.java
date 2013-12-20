package org.esa.beam.framework.ui.product.spectrum;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class SpectrumShapeProvider {

    private static final int starXPoints[] = {0, 1, 6, 2, 3, 0, -3, -2, -6, -1};
    private static final int starYPoints[] = {-6, -2, -2, 0, 5, 2, 5, 0, -2, -2};
    private static final int xXPoints[] = {-5, -4, 0, 4, 5, 1, 5, 4, 0, -4, -5, -1};
    private static final int xYPoints[] = {-4, -5, -1, -5, -4, 0, 4, 5, 1, 5, 4, 0};
    private static final int crossXPoints[] = {-5, -1, -1, 1, 1, 5, 5, 1, 1, -1, -1, -5};
    private static final int crossYPoints[] = {-1, -1, -5, -5, -1, -1, 1, 1, 5, 5, 1, 1};
    public static final Shape[] shapes = new Shape[]{
            new Polygon(),
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
    public static final int DEFAULT_SCALE_GRADE = 3;
    public static final Integer[] SCALE_GRADES = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final double[] scaleFactors = new double[]{0.6, 0.8, 1, 1.5, 2, 2.5, 3, 3.5, 4};

    private static ImageIcon[] convertShapesToIcons() {
        ImageIcon[] icons = new ImageIcon[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            icons[i] = convertShapeToIcon(shapes[i]);
        }
        return icons;
    }

    private static ImageIcon convertShapeToIcon(Shape seriesShape) {
        Rectangle rectangle = seriesShape.getBounds();
        if (rectangle.getWidth() > 0 && rectangle.getHeight() > 0) {
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
        return new ImageIcon();
    }

    public static Shape getScaledShape(int shapeIndex, int scaleGrade) {
        final Path2D.Double convertedShape = new Path2D.Double(shapes[shapeIndex]);
        final AffineTransform affineTransform = new AffineTransform();
        affineTransform.scale(scaleFactors[scaleGrade - 1], scaleFactors[scaleGrade - 1]);
        return convertedShape.createTransformedShape(affineTransform);
    }

}
