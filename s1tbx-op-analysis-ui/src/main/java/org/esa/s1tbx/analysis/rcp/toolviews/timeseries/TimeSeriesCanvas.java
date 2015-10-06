/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.ui.diagram.DiagramCanvas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

/**
 * The diagram canvas
 */
public class TimeSeriesCanvas extends DiagramCanvas {
    private final TimeSeriesSettings settings;

    public TimeSeriesCanvas(final TimeSeriesSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!(g instanceof Graphics2D)) {
            return;
        }

        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (settings.isShowingLegend() && getDiagram() != null) {
            drawLegend(g2d);
        }
    }

    private void drawLegend(final Graphics2D g2d) {
        final Rectangle graphArea = getDiagram().getGraphArea();
        int x0 = graphArea.x + 10, y0 = 20;
        g2d.setStroke(new BasicStroke(2.0f));
        final java.util.List<GraphData> graphDataList = settings.getGraphDataList();
        for (GraphData graphData : graphDataList) {
            if (graphData.getProducts() != null) {
                final Rectangle2D rect = getTextBox(g2d, graphData.getTitle(), x0, y0);
                drawTextBox(g2d, rect, graphData.getTitle(), x0, y0, graphData.getColor());
                x0 = (int) rect.getMaxX() + 10;
                if (x0 > getWidth() - 50) {
                    x0 = graphArea.x + 10;
                    y0 += 20;
                }
            }
        }
    }

    private Rectangle2D getTextBox(final Graphics2D g2d, final String text, final int x0, final int y0) {
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final Rectangle2D textBounds = fontMetrics.getStringBounds(text, g2d);
        Rectangle2D.Double r = new Rectangle2D.Double(x0 + textBounds.getX() - 2.0,
                                                      y0 + textBounds.getY() - 2.0,
                                                      textBounds.getWidth() + 4.0,
                                                      textBounds.getHeight() + 4.0);

        if (r.getMaxX() > getWidth()) {
            r.setRect(getWidth() - r.getWidth(), r.getY(), r.getWidth(), r.getHeight());
        }
        if (r.getMinX() < 0) {
            r.setRect(0, r.getY(), r.getWidth(), r.getHeight());
        }
        if (r.getMaxY() > getHeight()) {
            r.setRect(r.getX(), getHeight() - r.getHeight(), r.getWidth(), r.getHeight());
        }
        if (r.getMinY() < 0) {
            r.setRect(r.getX(), 0, r.getWidth(), r.getHeight());
        }
        return r;
    }

    private static void drawTextBox(final Graphics2D g2d, final Rectangle2D r,
                                    final String text, final int x0, final int y0, final Color color) {
        g2d.setColor(color);
        g2d.drawLine((int) r.getMinX(), (int) r.getMaxY(), (int) r.getMaxX(), (int) r.getMaxY());
        g2d.setColor(Color.black);
        g2d.drawString(text, x0, y0);
    }

    public boolean contains(int x, int y) {
        final TimeSeriesDiagram diagram = (TimeSeriesDiagram) getDiagram();
        String text = "";
      /*  if(diagram != null) {
            final Rectangle graphArea = diagram.getGraphArea();
            if (graphArea != null && y > graphArea.height) {
                final int numTicks = diagram.getXAxis().getNumMajorTicks();
                final double dist = (graphArea.width-graphArea.x) / numTicks;
                final int index = (int)((x - graphArea.x) / dist);
                final String[] tickText = diagram.getXAxis().createTickmarkTexts();
                if(index < tickText.length) {
                    final String refStr = tickText[index];
                    final Band band = findBand(diagram.getBands(), refStr);
                    final Product product = band.getProduct();
                    String timeStr = "";
                    if(product.getStartTime() != null)
                        timeStr = "<b>"+product.getStartTime().format()+"</b><br>";

                    text = "<html>"+
                            product.getProductRefString() +' '+ product.getName()+"<br>" +timeStr+
                            band.getName()+
                            "</html>";
                }
            }
        }   */
        setToolTipText(text);
        return super.contains(x, y);
    }

    private static Band findBand(final Band[] bands, final String refStr) {
        for (Band b : bands) {
            if (b.getProduct().getProductRefString().equals(refStr))
                return b;
        }
        return null;
    }
}
