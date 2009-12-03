package com.bc.ceres.swing.figure.support;

import junit.framework.TestCase;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

public class DefaultShapeFigureTest extends TestCase {
    public void testArea() {
        Area area = new Area(new Rectangle(0, 0, 100, 100));
        area.subtract(new Area(new Rectangle(25, 25, 50, 50)));
        area.add(new Area(new Rectangle(75, 75, 50, 50)));
        area.subtract(new Area(new Rectangle(87, 87, 25, 25)));
        area.subtract(new Area(new Rectangle(-26, -26, 50, 50)));
        show(area);

        show(new Line2D.Double(87, 87, 25, 25));
        show(new Rectangle(87, 87, 25, 25));
        show(new Ellipse2D.Float(87, 87, 25, 25));

        show(new Path2D.Double(new Rectangle(87, 87, 25, 25)));
        show(new Ellipse2D.Float(87, 87, 25, 25));


        Path2D path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        path.append(new Rectangle(12, 12, 25, 25), false);
        path.append(new Rectangle(65, 65, 25, 25), false);
        path.append(new Rectangle(0, 0, 100, 100), false);
        show(path);

        Area a2 = new Area();
        a2.add(new Area(new Rectangle(0, 0, 100, 100)));
        a2.subtract(new Area(new Rectangle(12, 12, 25, 25)));
        a2.subtract(new Area(new Rectangle(65, 65, 25, 25)));
        a2.add(new Area(new Rectangle(200, 200, 100, 100)));
        a2.subtract(new Area(new Rectangle(200 + 12, 200 + 12, 25, 25)));
        a2.subtract(new Area(new Rectangle(200 + 65, 200 + 65, 25, 25)));
        show(a2);
    }

    private void show(Shape shape) {
        System.out.println("Shape " + shape.getClass());
        PathIterator pathIterator = shape.getPathIterator(null);

        if (pathIterator.getWindingRule() == PathIterator.WIND_EVEN_ODD) {
            System.out.println("WIND_EVEN_ODD");
        } else if (pathIterator.getWindingRule() == PathIterator.WIND_NON_ZERO) {
            System.out.println("WIND_NON_ZERO");
        } else {
            System.out.println("WIND_?");
        }

        while (!pathIterator.isDone()) {

            double[] seg = new double[6];
            int i = pathIterator.currentSegment(seg);
            if (i == PathIterator.SEG_LINETO) {
                System.out.printf("SEG_LINETO: %s, %s%n", seg[0], seg[1]);
            } else if (i == PathIterator.SEG_MOVETO) {
                System.out.printf("SEG_MOVETO: %s, %s%n", seg[0], seg[1]);
            } else if (i == PathIterator.SEG_QUADTO) {
                System.out.printf("SEG_QUADTO: %s, %s, %s, %s%n", seg[0], seg[1], seg[2], seg[3]);
            } else if (i == PathIterator.SEG_CUBICTO) {
                System.out.printf("SEG_CUBICTO: %s, %s, %s, %s, %s, %s%n", seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
            } else if (i == PathIterator.SEG_CLOSE) {
                System.out.println("SEG_CLOSE");
            } else {
                System.out.println("SEG_?");
            }
            pathIterator.next();
        }
    }
}
