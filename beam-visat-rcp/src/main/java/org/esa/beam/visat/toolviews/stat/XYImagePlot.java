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

import com.bc.ceres.core.Assert;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * An X/Y plot that uses a buffered image to display its data.
 *
 * @author Norman Fomferra
 */
public class XYImagePlot extends XYPlot {

    private BufferedImage image;
    private Rectangle2D imageDataBounds;
    private final Object imageLock = new Object();

    public XYImagePlot() {
        super(null, new NumberAxis("X"), new NumberAxis("Y"), new XYLineAndShapeRenderer(false, false));
    }

    public BufferedImage getImage() {
        synchronized (imageLock) {
            return image;
        }
    }

    public void setImage(BufferedImage image) {
        synchronized (imageLock) {
            this.image = image;
            if (image != null && imageDataBounds == null) {
                setImageDataBounds(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            }
        }
    }

    public Rectangle2D getImageDataBounds() {
        synchronized (imageLock) {
            return imageDataBounds != null ? (Rectangle2D) imageDataBounds.clone() : null;
        }
    }

    public void setImageDataBounds(Rectangle2D imageDataBounds) {
        synchronized (imageLock) {
            this.imageDataBounds = (Rectangle2D) imageDataBounds.clone();
            DefaultXYDataset xyDataset = new DefaultXYDataset();
            xyDataset.addSeries("Image Data Bounds", new double[][]{
                    {imageDataBounds.getMinX(), imageDataBounds.getMaxX()},
                    {imageDataBounds.getMinY(), imageDataBounds.getMaxY()}
            });
            setDataset(xyDataset);
            getDomainAxis().setRange(imageDataBounds.getMinX(), imageDataBounds.getMaxX());
            getRangeAxis().setRange(imageDataBounds.getMinY(), imageDataBounds.getMaxY());
        }
    }

    @Override
    public boolean render(Graphics2D g2, Rectangle2D dataArea, int index, PlotRenderingInfo info, CrosshairState crosshairState) {
        final boolean foundData = super.render(g2, dataArea, index, info, crosshairState);
        if (image != null) {
            final int dx1 = (int) dataArea.getMinX();
            final int dy1 = (int) dataArea.getMinY();
            final int dx2 = (int) dataArea.getMaxX();
            final int dy2 = (int) dataArea.getMaxY();

            synchronized (imageLock) {
                final Rectangle rectangle = getImageSourceArea();
                final int sx1 = rectangle.x;
                final int sy1 = rectangle.y;
                final int sx2 = sx1 + rectangle.width - 1;
                final int sy2 = sy1 + rectangle.height - 1;
                g2.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
            }
        }
        return foundData;
    }

    final Rectangle getImageSourceArea() {
        Assert.notNull(image);
        Assert.notNull(imageDataBounds);
        final ValueAxis xAxis = getDomainAxis();
        final ValueAxis yAxis = getRangeAxis();
        final double scaleX = image.getWidth() / imageDataBounds.getWidth();
        final double scaleY = image.getHeight() / imageDataBounds.getHeight();
        final int x = crop(scaleX * (xAxis.getLowerBound() - imageDataBounds.getMinX()), 0, image.getWidth() - 1);
        final int y = crop(scaleY * (imageDataBounds.getMaxY() - yAxis.getUpperBound()), 0, image.getHeight() - 1);
        final int w = crop(scaleX * (xAxis.getUpperBound() - xAxis.getLowerBound()), 1, image.getWidth());
        final int h = crop(scaleY * (yAxis.getUpperBound() - yAxis.getLowerBound()), 1, image.getHeight());
        return new Rectangle(x, y, w, h);
    }

    private static int crop(double v, int i1, int i2) {
        int i = (int) Math.round(v);
        if (i < i1) {
            return i1;
        }
        if (i > i2) {
            return i2;
        }
        return i;
    }
}
