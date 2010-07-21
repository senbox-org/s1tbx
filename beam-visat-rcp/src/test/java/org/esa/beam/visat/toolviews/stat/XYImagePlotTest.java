/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import junit.framework.TestCase;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class XYImagePlotTest extends TestCase {

    public void testDefaultValues() {
        final XYImagePlot imagePlot = new XYImagePlot();

        assertNull(imagePlot.getImage());
        assertNull(imagePlot.getImageDataBounds());

        assertNull(imagePlot.getDataset());
        assertNotNull(imagePlot.getDomainAxis());
        assertNotNull(imagePlot.getRangeAxis());

        assertTrue(imagePlot.isDomainGridlinesVisible());
        assertTrue(imagePlot.isRangeGridlinesVisible());
    }

    public void testImage() {
        final XYImagePlot imagePlot = new XYImagePlot();

        final BufferedImage image = new BufferedImage(16, 9, BufferedImage.TYPE_INT_ARGB);
        imagePlot.setImage(image);
        assertSame(image, imagePlot.getImage());
        assertEquals(new Rectangle(0, 0, 16, 9), imagePlot.getImageDataBounds());

        final BufferedImage otherImage = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
        imagePlot.setImage(otherImage);
        assertSame(otherImage, imagePlot.getImage());
        assertEquals(new Rectangle(0, 0, 16, 9), imagePlot.getImageDataBounds());
    }

    public void testImageDataBounds() {
        final XYImagePlot imagePlot = new XYImagePlot();
        assertNull(imagePlot.getDataset());
        final Rectangle bounds = new Rectangle(0, 2, 20, 40);
        imagePlot.setImageDataBounds(bounds);
        assertNotSame(bounds, imagePlot.getImageDataBounds());
        assertEquals(bounds, imagePlot.getImageDataBounds());
        assertEquals(0.0, imagePlot.getDomainAxis().getLowerBound(), 1e-10);
        assertEquals(20.0, imagePlot.getDomainAxis().getUpperBound(), 1e-10);
        assertEquals(2.0, imagePlot.getRangeAxis().getLowerBound(), 1e-10);
        assertEquals(42.0, imagePlot.getRangeAxis().getUpperBound(), 1e-10);
        assertNotNull(imagePlot.getDataset());
    }

    public void testImageSourceArea() {
        final XYImagePlot imagePlot = new XYImagePlot();
        final BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        imagePlot.setImage(image);
        imagePlot.setImageDataBounds(new Rectangle2D.Double(-1.0, 0.0, 2.0, 1.0));

        imagePlot.getDomainAxis().setRange(-0.5, +0.5);
        imagePlot.getRangeAxis().setRange(0.25, 0.75);
        Rectangle area = imagePlot.getImageSourceArea();
        assertEquals(50, area.x);
        assertEquals(25, area.y);
        assertEquals(100, area.width);
        assertEquals(50, area.height);

        imagePlot.getDomainAxis().setRange(0, 0.1);
        imagePlot.getRangeAxis().setRange(0.5, 0.6);
        area = imagePlot.getImageSourceArea();
        assertEquals(100, area.x);
        assertEquals(40, area.y);
        assertEquals(10, area.width);
        assertEquals(10, area.height);

        imagePlot.getDomainAxis().setRange(0.5, 1.0);
        imagePlot.getRangeAxis().setRange(0.7, 1.0);
        area = imagePlot.getImageSourceArea();
        assertEquals(150, area.x);
        assertEquals(0, area.y);
        assertEquals(50, area.width);
        assertEquals(30, area.height);
    }

}