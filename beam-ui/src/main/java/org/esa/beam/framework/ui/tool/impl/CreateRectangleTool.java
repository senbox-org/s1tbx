/*
 * $Id: CreateRectangleTool.java,v 1.2 2006/11/22 13:05:36 marcop Exp $
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
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

//@todo 1 se/** - add (more) class documentation

public class CreateRectangleTool extends AbstractTwoPointTool {

    protected Rectangle _rectangle;

    public CreateRectangleTool() {
        this(new HashMap());
    }

    public CreateRectangleTool(Map figureAttributes) {
        super(figureAttributes);
        _rectangle = new Rectangle();
    }

    public void draw(Graphics2D g2d) {
        if (getNumPoints() > 0) {
            Stroke strokeOld = g2d.getStroke();
            g2d.setStroke(getStroke());
            Color colorOld = g2d.getColor();
            g2d.setColor(getColor());
            g2d.translate(0.5, 0.5);

            g2d.drawRect(_rectangle.x, _rectangle.y, _rectangle.width, _rectangle.height);

            g2d.translate(-0.5, -0.5);
            g2d.setStroke(strokeOld);
            g2d.setColor(colorOld);
        }
    }

    protected Figure createFigure(Map figureAttributes) {
        if (_rectangle.isEmpty()) {
            return null;
        }
        return ShapeFigure.createRectangleArea(_rectangle.x + 0.5F,
                                               _rectangle.y + 0.5F,
                                               _rectangle.width,
                                               _rectangle.height,
                                               figureAttributes);
    }

    protected void normalizeRectangle() {
        Point p1 = getFirstPoint();
        Point p2 = getLastPoint();
        _rectangle.x = p1.x;
        _rectangle.y = p1.y;
        _rectangle.width = p2.x - p1.x;
        _rectangle.height = p2.y - p1.y;
        if (_rectangle.width == 0) {
            _rectangle.width = 1;
        }
        if (_rectangle.width < 0) {
            _rectangle.x = p2.x;
            _rectangle.width *= -1;
        }
        if (_rectangle.height == 0) {
            _rectangle.height = 1;
        } else if (_rectangle.height < 0) {
            _rectangle.y = p2.y;
            _rectangle.height *= -1;
        }
    }
}
