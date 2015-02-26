/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.nestwwview;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwind.render.ScreenImage;
import java.awt.*;

import java.awt.image.BufferedImage;

/**

 */
public class ColorBarLegend extends gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend {

    public ColorBarLegend() {
        super();
    }

    public static ColorBarLegend fromColorGradient(int width, int height, double minValue, double maxValue, double minHue, double maxHue,
                                                   Color borderColor, Iterable<? extends LabelAttributes> labels, LabelAttributes titleLabel, boolean whiteZero)
    {
        System.out.println("fromColorGradient " + minHue + " " + maxHue);
        ColorBarLegend legend = new ColorBarLegend();
        legend.screenImage = new ScreenImage();
        legend.screenImage.setImageSource(legend.createColorGradientLegendImage(width,
                height, minHue, maxHue,
                borderColor, whiteZero));
        legend.labels = legend.createColorGradientLegendLabels(width,
                height, minValue, maxValue, labels, titleLabel);

        return legend;
    }


    protected BufferedImage createColorGradientLegendImage(int width, int
            height, double minHue, double maxHue, Color borderColor, boolean whiteZero)
    {
        System.out.println("createColorGradientLegendImage " + minHue + " " + maxHue);
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();
        try
        {
            for (int y = 0; y < height; y++)
            {
                double a = 1d - y / (double) (height - 1);
                double hue = WWMath.mix(a, minHue, maxHue);
                double sat = 1.0;
                if (whiteZero) {
                    sat = Math.abs(WWMath.mix(a, -1, 1));
                }
                g2d.setColor(Color.getHSBColor((float) hue, (float) sat, 1f));
                g2d.drawLine(0, y, width - 1, y);
            }

            if (borderColor != null)
            {
                g2d.setColor(borderColor);
                g2d.drawRect(0, 0, width - 1, height - 1);
            }
        }
        finally
        {
            g2d.dispose();
        }

        return image;
    }

    public void render (DrawContext dc) {
        //System.out.println("render");

        double x = dc.getView().getViewport().getWidth() - 75.0;
        double y = 320.0;

        setScreenLocation(new Point ((int) x, (int) y));
        super.render(dc);
    }
}