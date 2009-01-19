/*
 * $Id: SelectTool.java,v 1.1 2006/10/10 14:47:38 norman Exp $
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

import org.esa.beam.framework.draw.Drawable;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * A tool used to select items in a {@link org.esa.beam.framework.ui.product.ProductSceneView}.
 */
public class SelectTool extends AbstractTool {

    private static final Color RECT_COLOR = Color.YELLOW.brighter();
    private final Point selectionPoint = new Point();
    private final Rectangle selectionRect = new Rectangle();
    private Graphics graphics;

    @Override
    public Drawable getDrawable() {
        return null;
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    }

    @Override
    public void mouseClicked(ToolInputEvent e) {
        final AbstractTool selectToolDelegate = getDrawingEditor().getSelectTool();
        if (selectToolDelegate != null) {
            selectToolDelegate.mouseClicked(e);
        }
    }

    @Override
    public void mouseMoved(ToolInputEvent e) {
        final AbstractTool selectToolDelegate = getDrawingEditor().getSelectTool();
        if (selectToolDelegate != null) {
            selectToolDelegate.mouseMoved(e);
        }
    }

    @Override
    public void mousePressed(ToolInputEvent e) {
        final AbstractTool selectToolDelegate = getDrawingEditor().getSelectTool();
        if (selectToolDelegate != null) {
            selectToolDelegate.mousePressed(e);
        } else {
            graphics = e.getComponent().getGraphics();
            selectionPoint.setLocation(e.getMouseEvent().getPoint());
            adjustSelectionRect(e);
        }
    }

    @Override
    public void mouseDragged(ToolInputEvent e) {

        final AbstractTool selectToolDelegate = getDrawingEditor().getSelectTool();
        if (selectToolDelegate != null) {
            selectToolDelegate.mouseDragged(e);
        } else if (graphics != null) {
            graphics.setXORMode(RECT_COLOR);
            drawSelectionRect();
            adjustSelectionRect(e);
            drawSelectionRect();
            graphics.setPaintMode();
        }
    }

    @Override
    public void mouseReleased(ToolInputEvent e) {
        final AbstractTool selectToolDelegate = getDrawingEditor().getSelectTool();
        if (selectToolDelegate != null) {
            selectToolDelegate.mouseReleased(e);
        } else {
            getDrawingEditor().handleSelection(new Rectangle(selectionRect));
            if (graphics != null) {
                graphics.setXORMode(RECT_COLOR);
                drawSelectionRect();
                graphics.dispose();
            }
            graphics = null;
            selectionPoint.setLocation(0, 0);
            selectionRect.setBounds(0, 0, 0, 0);
        }
    }

    private void adjustSelectionRect(ToolInputEvent e) {
        int x = selectionPoint.x;
        int y = selectionPoint.y;
        int w = e.getMouseEvent().getX() - x;
        int h = e.getMouseEvent().getY() - y;
        if (w < 0) {
            w = -w;
            x -= w;
        }
        if (h < 0) {
            h = -h;
            y -= h;
        }
        selectionRect.setRect(x, y, w, h);
    }

    private void drawSelectionRect() {
        if (!selectionRect.isEmpty()) {
            graphics.drawRect(selectionRect.x, selectionRect.y,
                              selectionRect.width, selectionRect.height);
        }
    }
}
