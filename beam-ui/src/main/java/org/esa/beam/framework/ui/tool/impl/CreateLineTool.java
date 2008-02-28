/*
 * $Id: CreateLineTool.java,v 1.2 2006/11/22 13:05:36 marcop Exp $
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
package org.esa.beam.framework.ui.tool.impl;

import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

//@todo 1 se/** - add (more) class documentation

public class CreateLineTool extends AbstractTwoPointTool {

    public CreateLineTool() {
        this(new HashMap());
    }

    public CreateLineTool(Map figureAttributes) {
        super(figureAttributes);
    }

    protected Figure createFigure(Map figureAttributes) {
        if (getNumPoints() != 2) {
            return null;
        }
        Point p1 = getFirstPoint();
        Point p2 = getLastPoint();
        return ShapeFigure.createLine(p1.x + 0.5F,
                                      p1.y + 0.5F,
                                      p2.x + 0.5F,
                                      p2.y + 0.5F,
                                      figureAttributes);
    }

    /**
     * Draws this <code>Drawable</code> on the given <code>Graphics2D</code> drawing surface.
     *
     * @param g2d the graphics context
     */
    public void draw(Graphics2D g2d) {
        if (getNumPoints() > 0) {
            Point p1 = getFirstPoint();
            Point p2 = getLastPoint();

            Stroke strokeOld = g2d.getStroke();
            g2d.setStroke(getStroke());
            Color colorOld = g2d.getColor();
            g2d.setColor(getColor());
            g2d.translate(0.5, 0.5);

            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

            g2d.translate(-0.5, -0.5);
            g2d.setStroke(strokeOld);
            g2d.setColor(colorOld);
        }
    }
}
