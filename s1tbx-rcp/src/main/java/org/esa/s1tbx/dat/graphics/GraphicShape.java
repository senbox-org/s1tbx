package org.esa.s1tbx.dat.graphics;

import org.esa.s1tbx.dat.layers.ScreenPixelConverter;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Line2D;

/**
 * Helper for creating shapes
*/
public class GraphicShape {

    public static void drawArrow(final Graphics2D g, final ScreenPixelConverter screenPixel,
                                 final int x, final int y, final int x2, final int y2) {
        final double[] ipts = new double[8];
        final double[] vpts = new double[8];

        final int headSize = Math.max(5, (int) ((x2 - x) * 0.1));
        createArrow(x, y, x2, y2, headSize, ipts);

        screenPixel.pixelToScreen(ipts, vpts);

        //arrowhead
        g.draw(new Line2D.Double(vpts[4], vpts[5], vpts[2], vpts[3]));
        g.draw(new Line2D.Double(vpts[6], vpts[7], vpts[2], vpts[3]));
        final Polygon head = new Polygon();
        head.addPoint((int) vpts[4], (int) vpts[5]);
        head.addPoint((int) vpts[2], (int) vpts[3]);
        head.addPoint((int) vpts[6], (int) vpts[7]);
        head.addPoint((int) vpts[4], (int) vpts[5]);
        g.fill(head);
        //body
        g.draw(new Line2D.Double(vpts[0], vpts[1], vpts[2], vpts[3]));
    }

    private static void createArrow(int x, int y, int xx, int yy, int i1, double[] ipts) {
        ipts[0] = x;
        ipts[1] = y;
        ipts[2] = xx;
        ipts[3] = yy;
        final double d = xx - x;
        final double d1 = -(yy - y);
        double d2 = Math.sqrt(d * d + d1 * d1);
        final double d3;
        final double size = 2.0;
        if (d2 > (3.0 * i1))
            d3 = i1;
        else
            d3 = d2 / 3.0;
        if (d2 < 1.0)
            d2 = 1.0;
        if (d2 >= 1.0) {
            final double d4 = (d3 * d) / d2;
            final double d5 = -((d3 * d1) / d2);
            final double d6 = (double) xx - size * d4;
            final double d7 = (double) yy - size * d5;
            ipts[4] = (int) (d6 - d5);
            ipts[5] = (int) (d7 + d4);
            ipts[6] = (int) (d6 + d5);
            ipts[7] = (int) (d7 - d4);
        }
    }
}
