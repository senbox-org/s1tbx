/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.layers;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * Java 2D graphic utils
 */
public class GraphicsUtils {

    public static void setHighQuality(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static void shadowText(final Graphics2D g, final Color col, final String text, final int x, final int y) {
        g.setColor(Color.black);
        g.drawString(text, x + 2, y + 2);
        g.setColor(col);
        g.drawString(text, x, y);
    }

    public static void outlineText(final Graphics2D g, final Color col, final String text, final int x, final int y) {
        g.setColor(Color.black);
        g.drawString(text, x - 1, y - 1);
        g.drawString(text, x - 1, y + 1);
        g.drawString(text, x + 1, y - 1);
        g.drawString(text, x + 1, y + 1);
        g.setColor(col);
        g.drawString(text, x, y);
    }

    public static void highlightText(final Graphics2D g, final Color col, final String text, final int x, final int y,
                                     final Color highlightColor) {
        g.setColor(highlightColor);
        g.drawString(text, x - 1, y - 1);
        g.drawString(text, x - 1, y + 1);
        g.drawString(text, x + 1, y - 1);
        g.drawString(text, x + 1, y + 1);
        g.setColor(col);
        g.drawString(text, x, y);
    }

    public static String padString(final String text, final int width) {
        if(text.length() >= width || text.equalsIgnoreCase("NaN"))
            return text;
        final StringBuilder newText = new StringBuilder(width);
        final int firstPos = width - text.length();
        int i=0;
        while(i <= firstPos) {
            newText.append(' ');
            ++i;
        }
        newText.append(text);
        return newText.toString();
    }

    public static void createArrow(int x, int y, int xx, int yy, int i1, double[] ipts)
    {
        ipts[0] = x;
        ipts[1] = y;
        ipts[2] = xx;
        ipts[3] = yy;
        final double d = xx - x;
        final double d1 = -(yy - y);
        double d2 = Math.sqrt(d * d + d1 * d1);
        final double d3;
        final double size = 2.0;
        if(d2 > (3.0 * i1))
            d3 = i1;
        else
            d3 = d2 / 3.0;
        if(d2 < 1.0)
            d2 = 1.0;
        if(d2 >= 1.0) {
            final double d4 = (d3 * d) / d2;
            final double d5 = -((d3 * d1) / d2);
            final double d6 = (double)xx - size * d4;
            final double d7 = (double)yy - size * d5;
            ipts[4] = (int)(d6 - d5);
            ipts[5] = (int)(d7 + d4);
            ipts[6] = (int)(d6 + d5);
            ipts[7] = (int)(d7 - d4);
        }
    }

    public static void drawArrow(final Graphics2D g, final ScreenPixelConverter screenPixel,
                                 final int x, final int y, final int x2, final int y2) {
        final double[] ipts = new double[8];
        final double[] vpts = new double[8];

        final int headSize = Math.max(5, (int)((x2-x)*0.1));
        createArrow(x, y, x2, y2, headSize, ipts);

        screenPixel.pixelToScreen(ipts, vpts);

        //arrowhead
        g.draw(new Line2D.Double(vpts[4], vpts[5], vpts[2], vpts[3]));
        g.draw(new Line2D.Double(vpts[6], vpts[7], vpts[2], vpts[3]));
        final Polygon head = new Polygon();
        head.addPoint((int)vpts[4], (int)vpts[5]);
        head.addPoint((int)vpts[2], (int)vpts[3]);
        head.addPoint((int)vpts[6], (int)vpts[7]);
        head.addPoint((int)vpts[4], (int)vpts[5]);
        g.fill(head);
        //body
        g.draw(new Line2D.Double(vpts[0], vpts[1], vpts[2], vpts[3]));
    }
}
