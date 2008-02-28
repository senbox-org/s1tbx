/*
 * $Id: AbstractCreateLineTool.java,v 1.1 2006/10/10 14:47:37 norman Exp $
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.framework.ui.tool.ToolInputEvent;

//@todo 1 se/** - add (more) class documentation

public abstract class AbstractCreateLineTool extends AbstractCreateFigureTool {

    private final List _points;
    private final Point _currPoint;
    private final boolean _polyline;
    private final boolean _closed;

    public AbstractCreateLineTool(boolean polyline, boolean closed, Map figureAttributes) {
        super(figureAttributes);
        _points = new Vector();
        _currPoint = new Point();
        _polyline = polyline;
        _closed = closed;
    }

    public int getNumPoints() {
        return _points.size();
    }

    public Point getPointAt(int i) {
        if (i < 0 || i >= _points.size()) {
            return null;
        }
        return (Point) _points.get(i);
    }

    public Point getFirstPoint() {
        return getPointAt(0);
    }

    public Point getLastPoint() {
        return getPointAt(getNumPoints() - 1);
    }

    protected Point getCurrPoint() {
        return _currPoint;
    }

    protected void setCurrPoint(int pixelX, int pixelY) {
        _currPoint.setLocation(pixelX, pixelY);
    }

    protected void setCurrPoint(Point point) {
        _currPoint.setLocation(point);
    }

    protected void setCurrPoint(ToolInputEvent e) {
        int pixelX = e.getPixelX();
        int pixelY = e.getPixelY();
        if (isControlKeyDown(e)) {
            Point firstPoint = getFirstPoint();
            if (firstPoint != null) {
                int dx = Math.abs(firstPoint.x - pixelX);
                int dy = Math.abs(firstPoint.y - pixelY);
                if (dx < dy) {
                    pixelX = firstPoint.x + 32 * ((pixelX - firstPoint.x) / 32);
                } else {
                    pixelY = firstPoint.y + 32 * ((pixelY - firstPoint.y) / 32);
                }
            }
        }
        setCurrPoint(pixelX, pixelY);
    }

    public void appendPoint(int x, int y) {
        Point lastPoint = getLastPoint();
        if (lastPoint != null
            && lastPoint.x == x
            && lastPoint.y == y) {
            return;
        }
        _points.add(new Point(x, y));
    }

    public void appendPoint(Point point) {
        appendPoint(point.x, point.y);
    }

    public void removeLastPoint() {
        removePointAt(getNumPoints() - 1);
    }

    public void removePointAt(int i) {
        if (i < 0 || i >= _points.size()) {
            return;
        }
        _points.remove(i);
    }

    public boolean isClosed() {
        return _closed;
    }

    public List getPoints() {
        return _points;
    }

    public boolean isPolyline() {
        return _polyline;
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
        _points.clear();
    }

    protected void finish() {
        super.finish();
        _points.clear();
    }

    protected Figure createFigure(Map figureAttributes) {
        if (getNumPoints() < 2) {
            return null;
        }
        GeneralPath generalPath = new GeneralPath();
        Point point = getFirstPoint();
        generalPath.moveTo(point.x + 0.5f, point.y + 0.5f);
        for (int i = 1; i < getNumPoints(); i++) {
            point = getPointAt(i);
            generalPath.lineTo(point.x + 0.5f, point.y + 0.5f);
        }
        if (isClosed()) {
            generalPath.closePath();
            return ShapeFigure.createPolygonArea(generalPath, figureAttributes);
        } else {
            return ShapeFigure.createPolyline(generalPath, figureAttributes);
        }
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(ToolInputEvent event) {
        boolean finished = false;

        if (isSingleLeftClick(event)) {
            setCurrPoint(event);
            Point currPoint = getCurrPoint();
            appendPoint(currPoint);
            getDrawingEditor().repaintTool();
        } else if (isDoubleLeftClick(event)) {
            if (isPolyline() && isClosed() && getNumPoints() >= 3) {
                finished = true;
            } else if (getNumPoints() >= 2) {
                finished = true;
            }
        }

        if (!finished) {
            if (isPolyline() && isClosed() && getNumPoints() >= 3) {
                Point firstPoint = getFirstPoint();
                Point lastPoint = getLastPoint();
                if (lastPoint.x == firstPoint.x
                    && lastPoint.y == firstPoint.y) {
                    finished = true;
                }
            } else if (!isPolyline() && getNumPoints() == 2) {
                finished = true;
            }
        }

        if (finished) {
            finish(event);
        }
    }

//    /**
//     * Invoked when a mouse button is pressed on a component and then
//     * dragged.  Mouse drag events will continue to be delivered to
//     * the component where the first originated until the mouse button is
//     * released (regardless of whether the mouse position is within the
//     * bounds of the component).
//     */
//    public void mouseClicked(ToolInputEvent event) {
//        boolean finished = false;
//
//        if (isSingleLeftClick(event)) {
//            setCurrPoint(event);
//            Point currPoint = getCurrPoint();
//            appendPoint(currPoint);
//            getDrawingEditor().repaintTool();
//        } else if (isDoubleLeftClick(event)) {
//            if (isPolyline() && isClosed() && getNumPoints() >= 3) {
//                finished = true;
//            } else if (getNumPoints() >= 2) {
//                finished = true;
//            }
//        }
//
//        if (!finished) {
//            if (isPolyline() && isClosed() && getNumPoints() >= 3) {
//                Point firstPoint = getFirstPoint();
//                Point lastPoint = getLastPoint();
//                if (lastPoint.x == firstPoint.x
//                        && lastPoint.y == firstPoint.y) {
//                    finished = true;
//                }
//            } else if (!isPolyline() && getNumPoints() == 2) {
//                finished = true;
//            }
//        }
//
//        if (finished) {
//            finish(event);
//        }
//    }


    /**
     * Invoked when the mouse button has been moved on a component (with no buttons no down).
     */
    public void mouseMoved(ToolInputEvent e) {
        if (getNumPoints() > 0) {
            setCurrPoint(e);
            getDrawingEditor().repaintTool();
        }
    }

    public void draw(Graphics2D g2d) {
        if (getNumPoints() == 0) {
            return;
        }

        Stroke strokeOld = g2d.getStroke();
        g2d.setStroke(getStroke());
        Color colorOld = g2d.getColor();
        g2d.setColor(getColor());
        g2d.translate(0.5, 0.5);

        Point p1 = getFirstPoint();
        Point p2;
        for (int i = 1; i < getNumPoints(); i++) {
            p2 = getPointAt(i);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            p1 = p2;
        }
        p2 = getCurrPoint();
        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

        g2d.translate(-0.5, -0.5);
        g2d.setStroke(strokeOld);
        g2d.setColor(colorOld);
    }
}
