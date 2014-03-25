package org.esa.beam.framework.ui.product.spectrum;

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
import javax.swing.ImageIcon;
import org.esa.beam.util.ArrayUtils;

public class SpectrumShapeProvider {

    private static final int starXPoints[] = {0, 1, 6, 2, 3, 0, -3, -2, -6, -1};
    private static final int starYPoints[] = {-6, -2, -2, 0, 5, 2, 5, 0, -2, -2};
    private static final int xXPoints[] = {-5, -4, 0, 4, 5, 1, 5, 4, 0, -4, -5, -1};
    private static final int xYPoints[] = {-4, -5, -1, -5, -4, 0, 4, 5, 1, 5, 4, 0};
    private static final int crossXPoints[] = {-5, -1, -1, 1, 1, 5, 5, 1, 1, -1, -1, -5};
    private static final int crossYPoints[] = {-1, -1, -5, -5, -1, -1, 1, 1, 5, 5, 1, 1};
    private static final Shape[] shapes = new Shape[]{
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
    private static final ImageIcon[] shapeIcons = convertShapesToIcons();
    public static final int DEFAULT_SCALE_GRADE = 3;
    public static final int EMPTY_SHAPE_INDEX = 0;
    private static final Integer[] scale_grades = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
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
                    (int) (rectangle.getHeight() - rectangle.getY()), BufferedImage.TYPE_INT_ARGB);
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
        if(scaleGrade == DEFAULT_SCALE_GRADE) {
            return shapes[shapeIndex];
        } else {
            final Path2D.Double convertedShape = new Path2D.Double(shapes[shapeIndex]);
            final AffineTransform affineTransform = new AffineTransform();
            affineTransform.scale(scaleFactors[scaleGrade - 1], scaleFactors[scaleGrade - 1]);
            return convertedShape.createTransformedShape(affineTransform);
        }
    }

    public static int getValidIndex(int i, boolean allowEmptySymbol) {
        int symbolIndex = i % shapes.length;
        if (!allowEmptySymbol) {
            symbolIndex = Math.max(1, (symbolIndex + 1) % shapes.length);
        }
        return symbolIndex;
    }

    public static ImageIcon getShapeIcon(int symbolIndex) {
        return shapeIcons[symbolIndex];
    }

    public static ImageIcon[] getShapeIcons() {
        return shapeIcons;
    }

    public static Integer[] getScaleGrades() {
        return scale_grades;
    }

    public static int getShape(ImageIcon shapeIcon) {
        return ArrayUtils.getElementIndex(shapeIcon, shapeIcons);
    }
}
