/*
 * $Id: ShapeFigure.java,v 1.1 2006/10/10 14:47:22 norman Exp $
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
package org.esa.beam.framework.draw;

import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.esa.beam.util.Guardian;

/**
 * A figure which uses a <code>java.awt.Shape</code> for its representation.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class ShapeFigure extends AbstractFigure {

    private final Shape _shape;
    private final boolean _oneDimensional;

    /**
     * Constructs a shape figure for the given shape.
     */
    public ShapeFigure(Shape shape, boolean oneDimensional, Map attributes) {
        super(attributes);
        Guardian.assertNotNull("shape", shape);
        _shape = shape;
        _oneDimensional = oneDimensional;
    }

    /**
     * Gets a shape representation of this figure.
     *
     * @return a shape representation of this figure which is never <code>null</code>.
     */
    public Shape getShape() {
        return _shape;
    }

    /**
     * Gets the figure as an area. One-dimensional figures are returned as line strokes with a width of 1 unit.
     */
    public Area getAsArea() {
        if (getShape() instanceof Area) {
            return (Area) getShape();
        }
        if (isOneDimensional()) {
            return new Area(new BasicStroke(1.0F).createStrokedShape(getShape()));
        }
        return new Area(getShape());
    }

    /**
     * Gets the figure's center.
     */
    public Point2D getCenterPoint() {
        Rectangle2D bounds = getBounds();
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

    /**
     * Gets the bounding box of the figure
     */
    public Rectangle2D getBounds() {
        return _shape.getBounds2D();
    }

    /**
     * Checks if a point is inside the figure.
     */
    public boolean containsPoint(double x, double y) {
        return _shape.contains(x, y);
    }

    /**
     * Determines whether a figure is a (one-dimensional) line in a two-dimensional space.
     */
    public boolean isOneDimensional() {
        return _oneDimensional;
    }

    /**
     * Draws this <code>Drawable</code> on the given <code>Graphics2D</code> drawing surface.
     *
     * @param g2d the graphics context
     */
    public void draw(Graphics2D g2d) {
        if (!isOneDimensional()) {
            fillShape(g2d);
        }
        outlineShape(g2d);
    }

    /**
     * Fills the <code>Drawable</code> on the given <code>Graphics2D</code> drawing surface.
     *
     * @param g2d the graphics context
     */
    public void fillShape(Graphics2D g2d) {

        boolean filled = false;
        Stroke strokeNew = null;
        Stroke strokeOld = null;
        Paint paintNew = null;
        Paint paintOld = null;
        Composite compositeNew = null;
        Composite compositeOld = null;

        Object value = null;

        value = getAttribute(FILL_PAINT_KEY);
        if (value instanceof Paint) {
            paintNew = (Paint) value;
            paintOld = g2d.getPaint();
            g2d.setPaint(paintNew);
            filled = true;
        }

        value = getAttribute(FILL_STROKE_KEY);
        if (value instanceof Stroke) {
            strokeNew = (Stroke) value;
            strokeOld = g2d.getStroke();
            g2d.setStroke(strokeNew);
            filled = true;
        }

        value = getAttribute(FILL_COMPOSITE_KEY);
        if (value instanceof Composite) {
            compositeNew = (Composite) value;
            compositeOld = g2d.getComposite();
            g2d.setComposite(compositeNew);
        }

        value = getAttribute(FILLED_KEY);
        if (value instanceof Boolean) {
            filled = (Boolean) value;
        }

        if (filled) {
            g2d.fill(_shape);
        }

        if (compositeNew != null) {
            g2d.setComposite(compositeOld);
        }
        if (paintNew != null) {
            g2d.setPaint(paintOld);
        }
        if (strokeNew != null) {
            g2d.setStroke(strokeOld);
        }
    }

    /**
     * Draws the outline of this <code>Drawable</code> on the given <code>Graphics2D</code> drawing surface.
     *
     * @param g2d the graphics context
     */
    public void outlineShape(Graphics2D g2d) {

        boolean outlined = false;
        Stroke strokeNew = null;
        Stroke strokeOld = null;
        Paint paintNew = null;
        Paint paintOld = null;
        Composite compositeNew = null;
        Composite compositeOld = null;

        Object value = null;

        value = getAttribute(OUTL_COLOR_KEY);
        if (value instanceof Paint) {
            paintNew = (Paint) value;
            paintOld = g2d.getPaint();
            g2d.setPaint(paintNew);
            outlined = true;
        }

        value = getAttribute(OUTL_STROKE_KEY);
        if (value instanceof Stroke) {
            strokeNew = (Stroke) value;
            strokeOld = g2d.getStroke();
            g2d.setStroke(strokeNew);
            outlined = true;
        }

        value = getAttribute(OUTL_COMPOSITE_KEY);
        if (value instanceof Composite) {
            compositeNew = (Composite) value;
            compositeOld = g2d.getComposite();
            g2d.setComposite(compositeNew);
        }

        value = getAttribute(OUTLINED_KEY);
        if (value instanceof Boolean) {
            outlined = (Boolean) value;
        }

        if (outlined) {
            g2d.draw(_shape);
        }

        if (compositeNew != null) {
            g2d.setComposite(compositeOld);
        }
        if (paintNew != null) {
            g2d.setPaint(paintOld);
        }
        if (strokeNew != null) {
            g2d.setStroke(strokeOld);
        }
    }

    public static LineFigure createLine(float x1, float y1, float x2, float y2, Map attributes) {
        return new LineFigure(new Line2D.Float(x1, y1, x2, y2), attributes);
    }

    public static LineFigure createPolyline(GeneralPath path, Map attributes) {
        return new LineFigure(path, attributes);
    }

    public static AreaFigure createRectangleArea(float x, float y, float w, float h, Map attributes) {
        return new AreaFigure(new Rectangle2D.Float(x, y, w, h), attributes);
    }

    public static AreaFigure createEllipseArea(float x, float y, float w, float h, Map attributes) {
        return new AreaFigure(new Ellipse2D.Float(x, y, w, h), attributes);
    }

    public static AreaFigure createPolygonArea(GeneralPath path, Map attributes) {
        return new AreaFigure(path, attributes);
    }

    public static AreaFigure createArbitraryArea(Area area, Map attributes) {
        return new AreaFigure(area, attributes);
    }
}

