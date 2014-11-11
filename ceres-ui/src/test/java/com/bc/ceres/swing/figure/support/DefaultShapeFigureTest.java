/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.figure.support;

import org.junit.Ignore;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

@Ignore
public class DefaultShapeFigureTest {

    public static void main(String[] args) {
       /*
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
         */


        doit(Path2D.WIND_EVEN_ODD, false, false, false);
        doit(Path2D.WIND_EVEN_ODD, false, false, true);
        doit(Path2D.WIND_EVEN_ODD, false, true, false);
        doit(Path2D.WIND_EVEN_ODD, false, true, true);
        doit(Path2D.WIND_EVEN_ODD, true, false, false);
        doit(Path2D.WIND_EVEN_ODD, true, false, true);
        doit(Path2D.WIND_EVEN_ODD, true, true, false);
        doit(Path2D.WIND_EVEN_ODD, true, true, true);
        doit(Path2D.WIND_NON_ZERO, false, false, false);
        doit(Path2D.WIND_NON_ZERO, false, false, true);
        doit(Path2D.WIND_NON_ZERO, false, true, false);
        doit(Path2D.WIND_NON_ZERO, false, true, true);
        doit(Path2D.WIND_NON_ZERO, true, false, false);
        doit(Path2D.WIND_NON_ZERO, true, false, true);
        doit(Path2D.WIND_NON_ZERO, true, true, false);
        doit(Path2D.WIND_NON_ZERO, true, true, true);



           /*
        Area a2 = new Area();
        a2.add(new Area(new Rectangle(0, 0, 100, 100)));
        a2.subtract(new Area(new Rectangle(12, 12, 25, 25)));
        a2.subtract(new Area(new Rectangle(65, 65, 25, 25)));
        a2.add(new Area(new Rectangle(200, 200, 100, 100)));
        a2.subtract(new Area(new Rectangle(200 + 12, 200 + 12, 25, 25)));
        a2.subtract(new Area(new Rectangle(200 + 65, 200 + 65, 25, 25)));
        show(a2);
        */
    }

    private static void doit(int winding, boolean clockwise1, boolean clockwise2, boolean clockwise3) {
        Path2D path;
        path = new Path2D.Double(winding);
        path.append(rectPath(clockwise1, 0, 0, 100, 100), false);
        path.append(rectPath(clockwise2, 12, 12, 25, 25), false);
        path.append(rectPath(clockwise3, 65, 65, 25, 25), false);
        StringBuilder sb =new StringBuilder();
        if(winding == Path2D.WIND_EVEN_ODD) {
            sb.append("WEO");
        }else {
            sb.append("WNZ");
        }
        sb.append((" - "));
        sb.append(String.valueOf(clockwise1));
        sb.append((", "));
        sb.append(String.valueOf(clockwise2));
        sb.append((", "));
        sb.append(String.valueOf(clockwise3));
        show(path, sb.toString());
    }

    private static Path2D rectPath(boolean clockwise, int x, int y, int w, int h) {
        Path2D.Double linePath = new Path2D.Double();
        linePath.moveTo(x, y);
        if (clockwise) {
            linePath.lineTo(x, y + h);
            linePath.lineTo(x + w, y + h);
            linePath.lineTo(x + w, y);
        } else {
            linePath.lineTo(x + w, y);
            linePath.lineTo(x + w, y + h);
            linePath.lineTo(x, y + h);
        }
        linePath.lineTo(x, y);
        linePath.closePath();
        return linePath;
    }


    private static void show(final Shape shape, String title) {
        JFrame frame = new JFrame(title);
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.GRAY);
                    for (int i = 0; i < getWidth(); i+=10) {
                        g.drawLine(i, 0, i, getHeight());
                    }
                for (int i = 0; i < getHeight(); i+=10) {
                    g.drawLine( 0, i, getWidth(), i);
                }

                Graphics2D graphics2D = (Graphics2D) g;
                graphics2D.translate(10, 10);
                 graphics2D.setPaint(new Color(255, 255, 255, 150));
                graphics2D.fill(shape);
                graphics2D.setPaint(Color.BLUE);
                graphics2D.draw(shape);
            }
        };
        canvas.setBorder(new EmptyBorder(9,9,9,9));
        canvas.setPreferredSize(new Dimension(shape.getBounds().width+20, shape.getBounds().height+20));
        frame.getContentPane().add(canvas) ;
        frame.pack();
        frame.setVisible(true);

        System.out.println("Shape " +title);
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
