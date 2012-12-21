/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dat.views.polarview;

import org.esa.beam.util.Debug;
import org.esa.beam.util.math.MathUtils;

import java.awt.*;

class PolarCanvas extends Container {

    private final Axis radialAxis;
    private final Axis colourAxis;
    private PolarData data = null;
    private double rings[] = null;
    private String ringText[] = null;
    private final float dirOffset;
    private int plotRadius;
    private double windDirection = 0;
    private boolean showWindDirection = false;
    private boolean opaque;
    private final Dimension graphSize = new Dimension(200, 100);
    private Point origin = new Point(0, 0);

    private Image colorBar = null;
    private String axisLabel1 = "", axisLabel2 = "";

    public PolarCanvas() {
        this(new Axis(Axis.RADIAL), new Axis(Axis.RIGHT_Y));
    }

    private PolarCanvas(Axis radialAxis, Axis colourAxis) {
        opaque = false;
        dirOffset = 0.0F;
        plotRadius = 0;
        this.radialAxis = radialAxis;
        this.colourAxis = colourAxis;
        colourAxis.setLocation(4);
        radialAxis.setSpacing(0);
        colourAxis.setSpacing(0);

        enableEvents(16L);
        setBackground(Color.white);
    }

    public void setAxisNames(String name1, String name2) {
        axisLabel1 = name1;
        axisLabel2 = name2;
    }

    @Override
    public Font getFont() {
        final Font font = super.getFont();
        if (font == null)
            return Font.decode("SansSerif-plain-9");
        return font;
    }

    @Override
    public final void setBackground(Color background) {
        opaque = true;
        super.setBackground(background);
    }

    private void fillBackground(Graphics g) {
        final Rectangle clip = g.getClipBounds();
        final Color col = g.getColor();
        g.setColor(getBackground());
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
        g.setColor(col);
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
        try {
            super.repaint(tm, x, y, width, height);
        } catch (Throwable e) {
            Debug.trace(e);
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        try {
            if (isShowing()) {
                if (opaque)
                    fillBackground(g);
                g.setColor(getForeground());
                g.setFont(getFont());
                paintComponents(this, g);
            }
            drawSynchronised(g, getSize());
        } catch (Throwable e) {
            Debug.trace(e);
        }
    }

    private synchronized void drawSynchronised(Graphics g, Dimension size) {
        draw(g, size);
    }

    private Rectangle positionPlot(Dimension size, int x, int y, int bottom, int right) {
        //insets.setValue(top, left, bottom, right);
        //Rectangle r = insets.shrinkRect(size);
        final Rectangle r = new Rectangle(x, y,
                (int) size.getWidth() - x - right, (int) size.getHeight() - y - bottom);
        origin = r.getLocation();
        graphSize.setSize(r.width, r.height);
        return r;
    }

    private void loadColorBar(ColourScale scale) {
        if (colorBar == null)
            colorBar = createImage(new ColorBar(scale));
    }

    private void drawColorBar(Graphics g, Axis cAxis) {
        final Dimension cbSize = new Dimension((int) (graphSize.width * 0.03),
                (int) (Math.min(200, graphSize.height * 0.6)));
        final Point at = new Point(20, -100);

        g.translate(at.x, at.y);
        g.drawImage(colorBar, 0, 0, cbSize.width, cbSize.height, this);
        g.drawRect(0, 0, cbSize.width, cbSize.height);
        g.translate(cbSize.width, cbSize.height);
        cAxis.draw(g, cbSize);
        g.drawString(cAxis.getUnit(), 50, 5);
        g.translate(-cbSize.width - at.x, -cbSize.height - at.y);
    }

    @Override
    protected void finalize()
            throws Throwable {
        if (colorBar != null)
            colorBar.flush();
        colorBar = null;
        super.finalize();
    }

    private static void paintComponents(Container c, Graphics g) {
        if (!c.isShowing()) return;

        final int ncomponents = c.getComponentCount();
        final Rectangle clip = g.getClipBounds();

        int i = ncomponents - 1;
        while (i >= 0) {
            final Component component[] = c.getComponents();
            final Component comp = component[i];
            if (comp == null || !comp.isVisible())
                continue;
            final Rectangle bounds = comp.getBounds();
            Rectangle cr;
            if (clip == null)
                cr = new Rectangle(bounds);
            else
                cr = bounds.intersection(clip);
            if (cr.isEmpty())
                continue;

            final Graphics cg = g.create();
            cg.setClip(cr);
            cg.translate(bounds.x, bounds.y);
            try {
                comp.paint(cg);
            } catch (Throwable e) {
                //
            }

            cg.dispose();
            i--;
        }
    }

    public Axis getRadialAxis() {
        return radialAxis;
    }

    public Axis getColourAxis() {
        return colourAxis;
    }

    public PolarData getData() {
        return data;
    }

    public void setData(PolarData data) {
        this.data = data;
        if (data != null) {
            data.setRAxis(radialAxis);
            data.setDirOffset(dirOffset);
            data.setCAxis(colourAxis);
        }
    }

    public void setRings(double rings[], String ringText[]) {
        this.rings = rings;
        this.ringText = ringText;
    }

    public double[] getRTheta(Point oP) {
        final Point p = new Point(oP);
        p.y = origin.y - p.y;
        p.x -= origin.x;
        if (Math.abs(p.y) > plotRadius || Math.abs(p.x) > plotRadius) {
            return null;
        } else {
            final int r = (int) Math.sqrt(p.x * p.x + p.y * p.y);
            final double rV = data.valueFromScreenPoint(r);
            return new double[]{rV, (360D - (Math.atan2(p.x, p.y) * 180D) / Math.PI) % 360D};
        }
    }

    public Axis selectAxis(Point oP) {
        final Point p = new Point(oP);
        p.y = origin.y - p.y;
        p.x -= origin.x;
        if (Math.abs(p.y) < plotRadius) {
            if (Math.abs(p.x) < plotRadius)
                return radialAxis;
            if (p.x > graphSize.width / 2)
                return colourAxis;
        }
        return null;
    }

    private void draw(Graphics g, Dimension size) {
        final int annotationHeight = 100;
        final int x = Math.max((int) (size.height * 0.05), 10);
        final int y = Math.max((int) (size.width * 0.05), 10);
        final int bottom = Math.max((int) (size.height * 0.1) + annotationHeight, 20);
        final int right = Math.max((int) (size.width * 0.1), 20);
        final Rectangle r = positionPlot(size, x, y, bottom, right);
        plotRadius = Math.min(r.width / 2, r.height / 2);
        final Dimension quadrantSize = new Dimension(plotRadius, plotRadius);
        g.translate(0, origin.y + r.height);
        if (data != null) {
            loadColorBar(data.getColorScale());
            drawColorBar(g, colourAxis);
        }
        g.translate(0, -origin.y - r.height);

        origin.y += r.height / 2;
        origin.x += r.width / 2;
        final Graphics graphics = g.create();
        graphics.translate(origin.x, origin.y);
        radialAxis.setSize(quadrantSize);
        if (data != null)
            data.draw(graphics);
        if (rings != null) {
            int ri = 0;
            for (double ring : rings) {
                final int rad = radialAxis.getScreenPoint(ring);
                final int rad2 = rad + rad;
                graphics.setColor(Color.lightGray);
                graphics.drawOval(-rad, -rad, rad2, rad2);
                if(ringText != null && ringText[ri] != null) {
                    graphics.setColor(Color.black);
                    graphics.drawString(ringText[ri], 0, -rad);
                }
                ++ri;
            }
        } else {
            radialAxis.draw(graphics);
        }

        // draw wind direction & speed
        if(showWindDirection)
            drawWindDirection(graphics, plotRadius, windDirection-90);

        graphics.translate(-origin.x, -origin.y);

        drawAxisLabels(graphics);
        
        graphics.dispose();
    }

    private static void drawWindDirection(Graphics graphics, double radius, double theta) {
        final double a = theta * MathUtils.DTOR;
        final int x1 = (int)(radius * Math.cos(a));
        final int y1 = (int)(radius * Math.sin(a));
        final int x2 = (int)((radius+50) * Math.cos(a));
        final int y2 = (int)((radius+50) * Math.sin(a));

        graphics.setColor(Color.black);
        graphics.drawLine(x1, y1, x2, y2);

        drawArrowHead(graphics, x2, y2, theta, radius+40);
    }

    private void drawAxisLabels(Graphics graphics) {
        final int x = 20;
        final int y = origin.y;
        final int d = 50;

        graphics.setColor(Color.black);

        final int y2 = y-d;
        graphics.drawLine(x, y, x, y-d);
        graphics.drawLine(x, y2, x-5, y2+5);
        graphics.drawLine(x, y2, x+5, y2+5);
        graphics.drawString(axisLabel1, x-15, y2-10);        

        final int x2 = x+d;
        graphics.drawLine(x, y, x2, y);
        graphics.drawLine(x2, y, x2-5, y-5);
        graphics.drawLine(x2, y, x2-5, y+5);
        graphics.drawString(axisLabel2, x2-10, y+20);
    }

    private static void drawArrowHead(Graphics graphics, int x, int y, double theta, double length) {
        final double b = (theta + 1) * MathUtils.DTOR;
        final int x3 = (int)(length * Math.cos(b));
        final int y3 = (int)(length * Math.sin(b));
        graphics.drawLine(x, y, x3, y3);

        final double c = (theta - 1) * MathUtils.DTOR;
        final int x4 = (int)(length * Math.cos(c));
        final int y4 = (int)(length * Math.sin(c));
        graphics.drawLine(x, y, x4, y4);
    }

    public void setWindDirection(double dir) {
        windDirection = dir;
    }

    public void showWindDirection(boolean flag) {
        showWindDirection = flag;
    }
}
