/*
 * $Id: AbstractTwoPointTool.java,v 1.1 2006/10/10 14:47:37 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package org.esa.beam.framework.ui.tool.impl;

import java.awt.Point;
import java.util.Map;

import org.esa.beam.framework.ui.tool.ToolInputEvent;

//@todo 1 se/** - add (more) class documentation

public abstract class AbstractTwoPointTool extends AbstractCreateFigureTool {

    private final Point _firstPoint;
    private final Point _lastPoint;
    private int _numPoints;

    public AbstractTwoPointTool(Map figureAttributes) {
        super(figureAttributes);
        _firstPoint = new Point();
        _lastPoint = new Point();
    }

    public Point getFirstPoint() {
        return _firstPoint;
    }

    public Point getLastPoint() {
        return _lastPoint;
    }

    public int getNumPoints() {
        return _numPoints;
    }

    /**
     * Invoked when a mouse button is pressed on a component and then dragged.  Mouse drag events will continue to be
     * delivered to the component where the first originated until the mouse button is released (regardless of whether
     * the mouse position is within the bounds of the component).
     */
    public void mousePressed(ToolInputEvent event) {
        if (isSingleLeftClick(event) && _numPoints == 0) {
            setDragging(true);
            _firstPoint.x = event.getPixelX();
            _firstPoint.y = event.getPixelY();
            _lastPoint.x = _firstPoint.x;
            _lastPoint.y = _firstPoint.y;
            _numPoints++;
            normalizeRectangle();
            getDrawingEditor().repaintTool();
        }
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(ToolInputEvent event) {
        setDragging(false);
        if (isSingleLeftClick(event) && _numPoints == 1) {
            _lastPoint.x = event.getPixelX();
            _lastPoint.y = event.getPixelY();
            _numPoints++;
            normalizeRectangle();
            finish(event);
        }
    }

    /**
     * Invoked when the mouse button has been moved on a component (with no buttons no down).
     */
    public void mouseDragged(ToolInputEvent event) {
        if (isLeftMouseButtonDown(event) && _numPoints == 1) {
            setDragging(true);
            _lastPoint.x = event.getPixelX();
            _lastPoint.y = event.getPixelY();
            normalizeRectangle();
            getDrawingEditor().repaintTool();
        }
    }

    /**
     * Invoked when the mouse button has been moved on a component (with no buttons no down).
     */
    public void mouseMoved(ToolInputEvent event) {
        if (isDragging()) {
            mouseDragged(event);
        }
    }

    protected void normalizeRectangle() {
    }

    /**
     * Cancels the tool. This method is called whenever the user switches to another tool while this tool is active. Use
     * this method to do some clean-up when the tool is switched.
     * <p/>
     * <p>The default implementation simply calls <code>setCanceled(false)</code>.
     * <p/>
     * <p>Subclassers should always call <code>super.cancel()</code>.
     */
    public void cancel() {
        super.cancel();
        _numPoints = 0;
        setDragging(false);
    }

    protected void finish() {
        super.finish();
        _numPoints = 0;
        setDragging(false);
    }
}
