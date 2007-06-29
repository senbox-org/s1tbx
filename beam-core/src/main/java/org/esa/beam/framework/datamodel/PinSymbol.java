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

import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.util.Guardian;

import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

/**
 * This class represents a <code>{@link Pin}</code>'s shape.
 *
 * @author Sabine Embacher
 * @version $Revision: 1.2 $ $Date: 2006/10/10 14:47:21 $
 */
public class PinSymbol extends ShapeFigure {

    private final static String ATTRIB_NAME = "pinSymbolName";
    private final static String ATTRIB_KEY_ICON = "ICON";
    private final static String ATTRIB_KEY_REF_POINT = "REF_POINT";

    private static final float SELECTION_SCALING = 3.0f;
    private static final Color SELECTION_COLOR =  new Color(255, 255, 0, 200);

    public PinSymbol(String name, Shape shape) {
        super(shape, false, null);
        setName(name);
    }

    public void drawSelected(Graphics2D g2d) {
        draw(g2d, true);
    }

    @Override
    public void draw(Graphics2D g2d) {
        draw(g2d, false);
    }

    private void draw(Graphics2D g2d, boolean selected) {

        PixelPos refPoint = getRefPoint();
        double x0 = refPoint.getX();
        double y0 = refPoint.getY();

        g2d.translate(-x0, -y0);

        if (selected) {
            Stroke outlineStroke = getOutlineStroke();
            Paint outlinePaint = getOutlinePaint();
            Paint fillPaint = getFillPaint();

            float lineWidth = 1.0f;
            if (outlineStroke instanceof BasicStroke) {
                BasicStroke basicStroke = (BasicStroke) outlineStroke;
                lineWidth = basicStroke.getLineWidth();
            }
            if (lineWidth < 1.0f) {
                lineWidth = 1.0f;
            }

            setFillPaint(null);

            int[] alphas = new int[] {64, 128, 192};
            for (int i = 0; i < alphas.length; i++) {

                BasicStroke selectionStroke = new BasicStroke(lineWidth + 2 * (alphas.length - i));
                Color selectionColor = new Color(SELECTION_COLOR.getRed(),
                                                 SELECTION_COLOR.getGreen(),
                                                 SELECTION_COLOR.getBlue(),
                                                 alphas[i]);

                setOutlineStroke(selectionStroke);
                setOutlinePaint(selectionColor);
                super.draw(g2d);
            }

            setOutlinePaint(outlinePaint);
            setOutlineStroke(outlineStroke);
            setFillPaint(fillPaint);
        }

        super.draw(g2d);
        g2d.translate(x0, y0);
    }

    public String getName() {
        Object attribute = getAttribute(ATTRIB_NAME);
        if (attribute instanceof String) {
            return (String) attribute;
        }
        return null;
    }

    public void setName(final String name) {
        Guardian.assertNotNullOrEmpty("name", name);
        setAttribute(ATTRIB_NAME, name);
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
        Object attribute = getAttribute(ATTRIB_KEY_ICON);
        if (attribute instanceof ImageIcon) {
            return (ImageIcon) attribute;
        }
        return null;
    }

    public void setIcon(ImageIcon icon) {
        setAttribute(ATTRIB_KEY_ICON, icon);
    }

    public PixelPos getRefPoint() {
        Object attribute = getAttribute(ATTRIB_KEY_REF_POINT);
        if (attribute instanceof PixelPos) {
            return (PixelPos) attribute;
        }
        return new PixelPos();
    }

    public void setRefPoint(PixelPos refPoint) {
        Guardian.assertNotNull("refPoint", refPoint);
        setAttribute(ATTRIB_KEY_REF_POINT, refPoint);
    }

    public static PinSymbol createDefaultPinSymbol() {
        // define symbol constants
        //
        final float r = 14.0f;
        final float h = 24.0f;
        final Paint fillPaint = new Color(128, 128, 255);
        final Paint outlinePaint = new Color(0, 0, 64);
        final float lineWidth = 1.0f;

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
        pinSymbol.setOutlineStroke(new BasicStroke(lineWidth));
        pinSymbol.setRefPoint(new PixelPos(0, h));
//        pinSymbol.setRefPoint(new PixelPos(-strokeWidth, h - strokeWidth));
        return pinSymbol;
    }
}
