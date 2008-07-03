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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

/**
 * This class represents a <code>{@link Pin}</code>'s shape.
 *
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class PinSymbol extends ShapeFigure {

    private final static String ATTRIB_NAME = "pinSymbolName";
    private final static String ATTRIB_KEY_ICON = "ICON";
    private final static String ATTRIB_KEY_REF_POINT = "REF_POINT";

    private static final Color SELECTION_COLOR = new Color(255, 255, 0, 200);

    public PinSymbol(String name, ImageIcon icon) {
        this(name, new Rectangle(0, 0, icon.getIconWidth(), icon.getIconHeight()));
        setIcon(icon);
    }

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
            Color outlineColor = getOutlineColor();
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
                setOutlineColor(selectionColor);
                super.draw(g2d);
            }

            setOutlineColor(outlineColor);
            setOutlineStroke(outlineStroke);
            setFillPaint(fillPaint);
        }
        ImageIcon icon = getIcon();
        if (icon != null) {
            g2d.drawImage(icon.getImage(), null, null);
        } else {
            super.draw(g2d);
        }
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

    public Color getOutlineColor() {
        Object attribute = getAttribute(OUTL_COLOR_KEY);
        if (attribute instanceof Color) {
            return (Color) attribute;
        }
        return null;
    }

    public void setOutlineColor(Color outlineColor) {
        setAttribute(OUTL_COLOR_KEY, outlineColor);
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
        setAttribute(FILLED_KEY, Boolean.valueOf(fill));
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

        // create symbol shape
        //
        final float r = 14.0f;
        final float h = 24.0f;
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
        final PinSymbol pinSymbol = new PinSymbol("Pin-Symbol", symbolShape);
        pinSymbol.setFillPaint(new Color(128, 128, 255));
        pinSymbol.setFilled(true);
        pinSymbol.setOutlineColor(new Color(0, 0, 64));
        pinSymbol.setOutlineStroke(new BasicStroke(1.0f));
        pinSymbol.setRefPoint(new PixelPos(0, h));
        return pinSymbol;
    }

    public static PinSymbol createDefaultGcpSymbol() {

//        // create symbol shape
//        //
//        final float r = 20.0f;
//        final GeneralPath path = new GeneralPath();
//        path.moveTo(-r/2, 0);
//        path.lineTo(+r/2, 0);
//        path.closePath();
//        path.moveTo(0, -r/2);
//        path.lineTo(0, +r/2);
//        path.closePath();
//
//        // create symbol
//        //
//        final PinSymbol pinSymbol = new PinSymbol("GCP-Symbol", path);
//        pinSymbol.setFillPaint(new Color(255, 255, 255));
//        pinSymbol.setFilled(true);
//        pinSymbol.setOutlineColor(new Color(255, 255, 0));
//        pinSymbol.setOutlineStroke(new BasicStroke(1.0f));
//        pinSymbol.setRefPoint(new PixelPos(0, 0));
//        return pinSymbol;

        ImageIcon icon = new ImageIcon(PinSymbol.class.getResource("GcpShape.png"));
        PinSymbol pinSymbol = new PinSymbol("GCP-Symbol", icon);
        pinSymbol.setRefPoint(new PixelPos(icon.getIconWidth() * 0.5f, icon.getIconHeight() * 0.5f));
        return pinSymbol;

    }
}
