/*
 * $Id: AbstractCreateFigureTool.java,v 1.2 2006/11/22 13:05:36 marcop Exp $
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
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

/**
 * A base class for tools which are used to create figures in a drawing.
 */
public abstract class AbstractCreateFigureTool extends AbstractTool {

    private Stroke _stroke;
    private Color _color;
    private Map _figureAttributes;

    public AbstractCreateFigureTool() {
        this(new HashMap());
    }

    public AbstractCreateFigureTool(Map figureAttributes) {
        _stroke = new BasicStroke(0.0F);
        _color = Color.orange;
        _figureAttributes = figureAttributes;
    }

    public Map getFigureAttributes() {
        return _figureAttributes;
    }

    public void setFigureAttributes(Map figureAttributes) {
        _figureAttributes = figureAttributes;
    }

    public Stroke getStroke() {
        return _stroke;
    }

    public void setStroke(Stroke stroke) {
        _stroke = stroke;
    }

    public Color getColor() {
        return _color;
    }

    public void setColor(Color color) {
        _color = color;
    }

    /**
     * Gets the default cursor for this tool. The method simply returns <code>null</code>.
     *
     * @return the default cursor for this tool or <code>null</code> if this tool does not have a special cursor.
     */
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    protected abstract Figure createFigure(Map figureAttributes);

    protected void finish(ToolInputEvent toolInputEvent) {
        _figureAttributes.put(Figure.TOOL_INPUT_EVENT_KEY, toolInputEvent);
        finish();
    }

    protected void finish() {
        if (getDrawingEditor() != null) {
            Figure figure = createFigure(_figureAttributes);
            if (figure != null) {
                getDrawingEditor().addFigure(figure);
            }
        }
        super.finish();
    }
}
