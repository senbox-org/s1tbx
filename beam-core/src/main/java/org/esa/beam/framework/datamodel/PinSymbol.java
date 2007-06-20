/*
 * $Id: PinSymbol.java,v 1.2 2006/10/10 14:47:21 norman Exp $
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
package org.esa.beam.framework.datamodel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import javax.swing.ImageIcon;

import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.util.Guardian;

/**
 * This class represents a <code>{@link Pin}</code>'s shape.
 *
 * @author Sabine Embacher
 * @version $Revision: 1.2 $ $Date: 2006/10/10 14:47:21 $
 */
public class PinSymbol extends ShapeFigure {

    private final static String _ATTRIB_NAME = "pinSymbolName";
    private final static String _ATTRIB_KEY_ICON = "ICON";
    private final static String _ATTRIB_KEY_REF_POINT = "REF_POINT";

    public PinSymbol(String name, Shape shape) {
        super(shape, false, null);
        setName(name);
    }

    public void drawSelected(Graphics2D g2d) {
        draw(g2d, true);
    }

    public void draw(Graphics2D g2d) {
        draw(g2d, false);
    }

    private void draw(Graphics2D g2d, boolean selected) {
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        PixelPos refPoint = getRefPoint();
        g2d.translate(-refPoint.getX(), -refPoint.getY());
        if (selected) {
            drawSelection(g2d);
        }
        super.draw(g2d);
        g2d.translate(refPoint.getX(), refPoint.getY());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
    }

    private void drawSelection(Graphics2D g2d) {
        final Stroke oldStroke = g2d.getStroke();
        final Color oldColor = g2d.getColor();
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(new Color(100, 100, 0, 150));
        final Rectangle bounds = getShape().getBounds();
        bounds.grow(2, 2);
        bounds.setLocation(bounds.x + 1, bounds.y + 1);
        g2d.draw(bounds);
        g2d.setColor(new Color(255, 255, 0, 150));
        bounds.setLocation(bounds.x - 1, bounds.y - 1);
        g2d.draw(bounds);
        g2d.setColor(oldColor);
        g2d.setStroke(oldStroke);
    }

    public String getName() {
        Object attribute = getAttribute(_ATTRIB_NAME);
        if (attribute instanceof String) {
            return (String) attribute;
        }
        return null;
    }

    public void setName(final String name) {
        Guardian.assertNotNullOrEmpty("name", name);
        setAttribute(_ATTRIB_NAME, name);
    }

    public Stroke getOutlineStroke() {
        Object attribute = getAttribute(OUTL_STROKE_KEY);
        if (attribute instanceof Stroke) {
            return (Stroke) attribute;
        }
        return null;
    }

    public void setOutlineStroke(final Stroke outlineStroke) {
        setAttribute(OUTL_STROKE_KEY, outlineStroke);
    }

    public Paint getOutlinePaint() {
        Object attribute = getAttribute(OUTL_PAINT_KEY);
        if (attribute instanceof Paint) {
            return (Paint) attribute;
        }
        return null;
    }

    public void setOutlinePaint(Paint outlinePaint) {
        setAttribute(OUTL_PAINT_KEY, outlinePaint);
    }

    public Paint getFillPaint() {
        Object attribute = getAttribute(FILL_PAINT_KEY);
        if (attribute instanceof Paint) {
            return (Paint) attribute;
        }
        return null;
    }

    public void setFillPaint(Paint fillPaint) {
        setAttribute(FILL_PAINT_KEY, fillPaint);
    }

    public boolean isFilled() {
        Object attribute = getAttribute(FILLED_KEY);
        if (attribute instanceof Boolean) {
            return (Boolean) attribute;
        }
        return false;
    }

    public void setFilled(boolean fill) {
        setAttribute(FILLED_KEY, new Boolean(fill));
    }

    public ImageIcon getIcon() {
        Object attribute = getAttribute(_ATTRIB_KEY_ICON);
        if (attribute instanceof ImageIcon) {
            return (ImageIcon) attribute;
        }
        return null;
    }

    public void setIcon(ImageIcon icon) {
        setAttribute(_ATTRIB_KEY_ICON, icon);
    }

    public PixelPos getRefPoint() {
        Object attribute = getAttribute(_ATTRIB_KEY_REF_POINT);
        if (attribute == null) {
            attribute = new PixelPos();
        }
        return (PixelPos) attribute;
    }

    public void setRefPoint(PixelPos refPoint) {
        Guardian.assertNotNull("refPoint", refPoint);
        setAttribute(_ATTRIB_KEY_REF_POINT, refPoint);
    }

    public static PinSymbol createDefaultPinSymbol() {
        // define symbol constants
        //
        final float r = 10.0f;
        final float h = 20.0f;
        final Paint fillPaint = new Color(128, 128, 255);
        final Paint outlinePaint = new Color(0, 0, 64);
        final float strokeWidth = 0.5f;

        // create symbol shape
        //
        final float h34 = 3 * h / 4;
        final float h14 = 1 * h / 4;
        final GeneralPath path = new GeneralPath();
        path.moveTo(0, h);
        path.lineTo(h34 - 1, h14 - 1);
        path.lineTo(h34 + 1, h14 + 1);
        path.closePath();
        final Ellipse2D.Float knob = new Ellipse2D.Float(h34 - r / 2, h14 - r / 2, r, r);
        final Area needle = new Area(path);
        needle.subtract(new Area(knob));
        final GeneralPath symbolShape = new GeneralPath();
        symbolShape.append(needle, false);
        symbolShape.append(knob, false);

        // create symbol
        //
        final PinSymbol pinSymbol = new PinSymbol("Default", symbolShape);
        pinSymbol.setFillPaint(fillPaint);
        pinSymbol.setFilled(true);
        pinSymbol.setOutlinePaint(outlinePaint);
        pinSymbol.setOutlineStroke(new BasicStroke(strokeWidth));
        pinSymbol.setRefPoint(new PixelPos(0, h));
//        pinSymbol.setRefPoint(new PixelPos(-strokeWidth, h - strokeWidth));
        return pinSymbol;
    }
}
