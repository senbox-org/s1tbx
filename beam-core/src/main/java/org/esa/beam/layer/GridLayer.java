/*
 * $Id: GridLayer.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.layer;

import com.bc.view.ViewModel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 */
public class GridLayer extends StyledLayer {
    private Rectangle2D rectangle;
    private int majorColCount;
    private int majorRowCount;
    private int minorColCount;
    private int minorRowCount;

    public GridLayer(Rectangle2D rectangle, int majorColCount, int majorRowCount, int minorColCount, int minorRowCount) {
        this.rectangle = rectangle;
        this.majorColCount = majorColCount;
        this.majorRowCount = majorRowCount;
        this.minorColCount = minorColCount;
        this.minorRowCount = minorRowCount;
    }

    @Override
    public String getPropertyNamePrefix() {
        return "grid";
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return rectangle;
    }

    public void draw(Graphics2D g2d, ViewModel viewModel) {

        final Graphics2D graphics = (Graphics2D) g2d.create();

        final int majorColCountTot = majorColCount * minorColCount;
        final int minorRowCountTot = majorRowCount * minorRowCount;
        float dashSize = (float) Math.max(rectangle.getWidth() / majorColCountTot,
                                          rectangle.getHeight() / minorRowCountTot) / 5.0f;
        if (dashSize < 1.0f) {
            dashSize = 1.0f;
        }

        BasicStroke stroke;

        stroke = new BasicStroke(0);
//            stroke = new BasicStroke(0,
//                                BasicStroke.CAP_BUTT,
//                                BasicStroke.JOIN_BEVEL,
//                                1f,
//                                new float[]{dashSize, dashSize}, 0.5f * dashSize);
        graphics.setStroke(stroke);
        graphics.setColor(Color.blue);

        drawGrid(graphics, rectangle, majorColCount * minorColCount, majorRowCount * minorRowCount);

        stroke = new BasicStroke(0);
        graphics.setStroke(stroke);
        graphics.setColor(Color.black);

        drawGrid(graphics, rectangle, majorColCount, majorRowCount);
        drawText(graphics, rectangle, majorColCount, majorRowCount);


        graphics.dispose();

    }

    public static void drawGrid(final Graphics2D graphics, final Rectangle2D rectangle, final int colCount, final int rowCount) {
        Line2D l = new Line2D.Double();

        double deltaX = rectangle.getWidth() / colCount;
        for (int i = 0; i <= colCount; i++) {
            double x = rectangle.getX() + i * deltaX;
            l.setLine(x, rectangle.getY(), x, rectangle.getY() + rectangle.getHeight());
            graphics.draw(l);
        }

        double deltaY = rectangle.getHeight() / rowCount;
        for (int i = 0; i <= rowCount; i++) {
            double y = rectangle.getY() + i * deltaY;
            l.setLine(rectangle.getX(), y, rectangle.getX() + rectangle.getWidth(), y);
            graphics.draw(l);
        }
    }

    public static void drawText(final Graphics2D graphics, final Rectangle2D rectangle, final int colCount, final int rowCount) {

        double deltaX = rectangle.getWidth() / colCount;
        for (int i = 0; i <= colCount; i++) {
            final double x = rectangle.getX() + i * deltaX;
            final double y1 = rectangle.getY();
            final double y2 = rectangle.getY() + rectangle.getHeight();
            final String s = String.valueOf(x);
            final Rectangle2D sBox = graphics.getFontMetrics().getStringBounds(s, graphics);
            final double sx = x - (sBox.getX() + 0.5 * sBox.getWidth());
            final double sy1 = y1 - (sBox.getY() + sBox.getHeight() + 5.);
            final double sy2 = y2 - (sBox.getY() - 5.);
            graphics.drawString(s, (float) sx, (float) sy1);
            graphics.drawString(s, (float) sx, (float) sy2);
        }

        double deltaY = rectangle.getHeight() / rowCount;
        for (int i = 0; i <= rowCount; i++) {
            final double y = rectangle.getY() + i * deltaY;
            final double x1 = rectangle.getX();
            final double x2 = rectangle.getX() + rectangle.getWidth();
            final String s = String.valueOf(y);
            final Rectangle2D sBox = graphics.getFontMetrics().getStringBounds(s, graphics);
            final double sx1 = x1 - (sBox.getX() + sBox.getWidth() + 5.);
            final double sx2 = x2 - (sBox.getX() - 5.);
            final double sy = y - (sBox.getY() + 0.5 * sBox.getHeight());
            graphics.drawString(s, (float) sx1, (float) sy);
            graphics.drawString(s, (float) sx2, (float) sy);
        }
    }
}
