/*
 * $Id: Diagram.java,v 1.1 2006/10/10 14:47:36 norman Exp $
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
package org.esa.beam.framework.ui.diagram;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.esa.beam.util.Guardian;

/**
 * The <code>Diagram</code> class is used to plot simple X/Y graphs. Instances of this class are composed of
 * <code>{@link DiagramValues}</code> and two <code>{@link DiagramAxis}</code> objects for the X and Y axes.
 */
public class Diagram {

    // @todo 2 nf/he - see StatisticsDialog for similar declarations (code smell!)
    private final static String _FONT_NAME = "Verdana";
    private final static int _FONT_SIZE = 9;
    private static final Color _DIAGRAM_BG_COLOR = new Color(200, 200, 255);
    private static final Color _DIAGRAM_FG_COLOR = new Color(0, 0, 100);
    private static final Color _DIAGRAM_TEXT_COLOR = Color.black;

    private DiagramValues _values;
    private DiagramAxis _xAxis;
    private DiagramAxis _yAxis;

    private final AxesPCL _axesPCL;

    private Font _font;
    private int _textGap;
    private int _majorTickLength;
    private int _minorTickLength;
    private boolean _valid;

    // Dependent properties
    //
    private FontMetrics _fontMetrics;
    private Rectangle _graphArea;
    private String[] _yTickTexts;
    private String[] _xTickTexts;
    private int _maxYTickTextWidth;

    public Diagram() {
        _axesPCL = new AxesPCL();
        _font = new Font(_FONT_NAME, Font.PLAIN, _FONT_SIZE);
        _textGap = 3;
        _majorTickLength = 5;
        _minorTickLength = 3;
    }

    public Diagram(DiagramAxis xAxis, DiagramAxis yAxis, DiagramValues values) {
        this();
        setXAxis(xAxis);
        setYAxis(yAxis);
        setValues(values);
    }

    public DiagramAxis getXAxis() {
        return _xAxis;
    }

    public void setXAxis(DiagramAxis xAxis) {
        Guardian.assertNotNull("xAxis", xAxis);
        DiagramAxis oldAxis = _xAxis;
        if (oldAxis != xAxis) {
            if (oldAxis != null) {
                oldAxis.removePropertyChangeListener(_axesPCL);
            }
            _xAxis = xAxis;
            _xAxis.addPropertyChangeListener(_axesPCL);
            invalidate();
        }
    }

    public DiagramAxis getYAxis() {
        return _yAxis;
    }

    public void setYAxis(DiagramAxis yAxis) {
        Guardian.assertNotNull("yAxis", yAxis);
        DiagramAxis oldAxis = _yAxis;
        if (oldAxis != yAxis) {
            if (oldAxis != null) {
                oldAxis.removePropertyChangeListener(_axesPCL);
            }
            _yAxis = yAxis;
            _yAxis.addPropertyChangeListener(_axesPCL);
            invalidate();
        }
    }

    public DiagramValues getValues() {
        return _values;
    }

    public void setValues(DiagramValues values) {
        Guardian.assertNotNull("_values", values);
        _values = values;
        invalidate();
    }

    public Font getFont() {
        return _font;
    }

    public void setFont(Font font) {
        _font = font;
        invalidate();
    }

    public int getTextGap() {
        return _textGap;
    }

    public void setTextGap(int textGap) {
        _textGap = textGap;
        invalidate();
    }

    public boolean isValid() {
        return _valid;
    }

    public void setValid(boolean valid) {
        _valid = valid;
    }

    public void invalidate() {
        setValid(false);
    }

    public void draw(Graphics2D g2D, int x, int y, int width, int height) {
        Object oldAntiAliasingRenderingHint = g2D.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Font oldFont = g2D.getFont();
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setFont(_font);
        _fontMetrics = g2D.getFontMetrics();

        validate(g2D, x, y, width, height);
        drawAxis(g2D, x, y, width, height);
        drawGraph(g2D, x, y, width, height);

        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntiAliasingRenderingHint);
        g2D.setFont(oldFont);
    }

    private void validate(Graphics2D g2D, int x, int y, int width, int height) {
        if (isValid()) {
            return;
        }

        _xTickTexts = _xAxis.createTickmarkTexts();
        _yTickTexts = _yAxis.createTickmarkTexts();

        // define y-Axis _values
        final int fontAscent = _fontMetrics.getAscent();

        _maxYTickTextWidth = 0;
        for (int i = 0; i < _yTickTexts.length; i++) {
            int sw = _fontMetrics.stringWidth(_yTickTexts[i]);
            _maxYTickTextWidth = Math.max(_maxYTickTextWidth, sw);
        }

        final int widthMaxX = _fontMetrics.stringWidth(_xTickTexts[_xTickTexts.length - 1]);

        int x1 = _textGap + fontAscent + _textGap + _maxYTickTextWidth + _textGap + _majorTickLength;
        int y1 = _textGap + fontAscent / 2;
        int x2 = x + width - (_textGap + widthMaxX / 2);
        int y2 = y + height - (_textGap + fontAscent + _textGap + fontAscent + _textGap + _majorTickLength);
        final int w = x2 - x1 + 1;
        final int h = y2 - y1 + 1;
        _graphArea = new Rectangle(x1, y1, w, h);
        setValid(w > 0 && h > 0);
    }


    private void drawGraph(Graphics2D g2D, int x, int y, int width, int height) {
        if (!isValid()) {
            return;
        }

        double xa, xb;
        double xa1 = _xAxis.getMinValue();
        double xa2 = _xAxis.getMaxValue();
        double xb1 = _graphArea.x;
        double xb2 = _graphArea.x + _graphArea.width;

        double ya, yb;
        double ya1 = _yAxis.getMinValue();
        double ya2 = _yAxis.getMaxValue();
        double yb1 = _graphArea.y + _graphArea.height;
        double yb2 = _graphArea.y;

        final Rectangle clipBounds = g2D.getClipBounds();
        g2D.setClip(_graphArea.x, _graphArea.y, _graphArea.width, _graphArea.height);

        g2D.setColor(_DIAGRAM_FG_COLOR);
        int x1, y1, x2 = 0, y2 = 0;
        int n = 0;
        if (_values != null) {
            n = _values.getNumValues();
        }
//        System.out.println("Diagram.drawGraph");
//        System.out.println("n = " + n);
//        System.out.println("xa1 = " + xa1);
//        System.out.println("ya1 = " + ya1);
//        System.out.println("xa2 = " + xa2);
//        System.out.println("ya2 = " + ya2);
//        System.out.println("xb1 = " + xb1);
//        System.out.println("yb1 = " + yb1);
//        System.out.println("xb2 = " + xb2);
//        System.out.println("yb2 = " + yb2);
        for (int i = 0; i < n; i++) {
            xa = _values.getXValueAt(i);
            ya = _values.getYValueAt(i);
            xb = xb1 + ((xa - xa1) * (xb2 - xb1)) / (xa2 - xa1);
            yb = yb1 + ((ya - ya1) * (yb2 - yb1)) / (ya2 - ya1);
            x1 = x2;
            y1 = y2;
            x2 = (int) Math.round(xb);
            y2 = (int) Math.round(yb);
            if (i > 0) {
                g2D.drawLine(x1, y1, x2, y2);
            }
//            System.out.println("xa = " + xa);
//            System.out.println("ya = " + ya);
//            System.out.println("xb = " + xb);
//            System.out.println("yb = " + yb);
//            System.out.println("x1 = " + x1);
//            System.out.println("y1 = " + y1);
//            System.out.println("x2 = " + x2);
//            System.out.println("y2 = " + y2);
            g2D.drawRect(x2 - 1, y2 - 1, 3, 3);
        }

        g2D.setClip(clipBounds);
    }

    private void drawAxis(Graphics2D g2D, int xOffset, int yOffset, int width, int height) {
        if (!isValid()) {
            return;
        }

        g2D.setColor(_DIAGRAM_BG_COLOR);
        g2D.fillRect(_graphArea.x, _graphArea.y, _graphArea.width, _graphArea.height);
        g2D.setColor(Color.black);
        g2D.drawRect(_graphArea.x, _graphArea.y, _graphArea.width, _graphArea.height);

        g2D.setColor(_DIAGRAM_TEXT_COLOR);

        int tw;
        int x0, y0, x1, x2, y1, y2, xMin, xMax, yMin, yMax, n, n1, n2;
        String text;

        final int th = _fontMetrics.getAscent();
        // draw X major tick lines
        xMin = _graphArea.x;
        xMax = _graphArea.x + _graphArea.width;
        y1 = _graphArea.y + _graphArea.height;
        n1 = _xAxis.getNumMajorTicks();
        n2 = _xAxis.getNumMinorTicks();
        n = (n1 - 1) * (n2 + 1) + 1;
        for (int i = 0; i < n; i++) {
            x0 = xMin + (i * (xMax - xMin)) / (n - 1);
            if (i % (n2 + 1) == 0) {
                y2 = y1 + _majorTickLength;
                text = _xTickTexts[i / (n2 + 1)];
                tw = _fontMetrics.stringWidth(text);
                g2D.drawString(text, x0 - tw / 2, y2 + _textGap + _fontMetrics.getAscent());
            } else {
                y2 = y1 + _minorTickLength;
            }
            g2D.drawLine(x0, y1, x0, y2);
        }

        // draw Y major tick lines
        x1 = _graphArea.x;
        yMin = _graphArea.y;
        yMax = _graphArea.y + _graphArea.height;
        n1 = _yAxis.getNumMajorTicks();
        n2 = _yAxis.getNumMinorTicks();
        n = (n1 - 1) * (n2 + 1) + 1;
        for (int i = 0; i < n; i++) {
            y0 = yMin + (i * (yMax - yMin)) / (n - 1);
            if (i % (n2 + 1) == 0) {
                x2 = x1 - _majorTickLength;
                text = _yTickTexts[n1 - 1 - (i / (n2 + 1))];
                tw = _fontMetrics.stringWidth(text);
                g2D.drawString(text, x2 - _textGap - tw, y0 + th / 2);
            } else {
                x2 = x1 - _minorTickLength;
            }
            g2D.drawLine(x1, y0, x2, y0);
        }

        // draw X axis name and unit
        text = _xAxis.getName() + " [" + _xAxis.getUnit() + "]";
        tw = _fontMetrics.stringWidth(text);
        x1 = _graphArea.x + _graphArea.width / 2 - tw / 2;
        y1 = yOffset + height - _textGap;
        g2D.drawString(text, x1, y1);

        // draw Y axis name and unit
        text = _yAxis.getName() + " [" + _yAxis.getUnit() + "]";
        tw = _fontMetrics.stringWidth(text);
        x1 = _graphArea.x - _majorTickLength - _textGap - _maxYTickTextWidth - _textGap;
        y1 = _graphArea.y + _graphArea.height / 2 + tw / 2;
        final AffineTransform oldTransform = g2D.getTransform();
        g2D.translate(x1, y1);
        g2D.rotate(-Math.PI / 2);
        g2D.drawString(text, 0, 0);
        g2D.setTransform(oldTransform);
    }


    private class AxesPCL implements PropertyChangeListener {

        /**
         * This method gets called when a bound property is changed.
         *
         * @param evt A PropertyChangeEvent object describing the event source and the property that has changed.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            invalidate();
        }
    }
}
