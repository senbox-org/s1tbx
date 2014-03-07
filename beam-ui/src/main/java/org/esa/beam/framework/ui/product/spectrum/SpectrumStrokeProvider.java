package org.esa.beam.framework.ui.product.spectrum;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import org.esa.beam.util.ArrayUtils;

public class SpectrumStrokeProvider {

    private static final Stroke[] strokes = new Stroke[]{
            new BasicStroke(),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{1.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 1.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{20.0f, 5.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{1.0f, 5.0f}, 0.0f),
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f, 1.0f, 10.0f}, 0.0f)
    };
    private static final ImageIcon[] strokeIcons = convertStrokesToIcons();
    public static final Stroke EMPTY_STROKE = new EmptyStroke();

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

    public static Stroke getStroke(int i) {
        return strokes[i % strokes.length];
    }

    public static ImageIcon getStrokeIcon(Stroke lineStyle) {
        return strokeIcons[ArrayUtils.getElementIndex(lineStyle, strokes)];
    }

    public static ImageIcon[] getStrokeIcons() {
        return strokeIcons;
    }

    public static Stroke getStroke(ImageIcon strokeIcon) {
        return strokes[ArrayUtils.getElementIndex(strokeIcon, strokeIcons)];
    }

    private static class EmptyStroke implements Stroke {

        @Override
        public Shape createStrokedShape(Shape p) {
            return new GeneralPath();
        }
    }

}
