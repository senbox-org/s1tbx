package org.esa.beam.visat.toolviews.stat;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

class XYImagePlot extends XYPlot {

    private BufferedImage image;
    private Rectangle2D imageDataBounds;

    public XYImagePlot() {

        NumberAxis xAxis = new NumberAxis("X");
        xAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);
        setDomainAxis(xAxis);

        NumberAxis yAxis = new NumberAxis("Y");
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        yAxis.setLowerMargin(0.0);
        yAxis.setUpperMargin(0.0);
        setRangeAxis(yAxis);

        setBackgroundPaint(Color.WHITE);
        setBackgroundAlpha(1.0f);       
        setDomainGridlinesVisible(true);
        setRangeGridlinesVisible(true);
    }

    public synchronized BufferedImage getImage() {
        return image;
    }

    public synchronized void setImage(BufferedImage image) {
        this.image = image;
        if (image != null && imageDataBounds == null) {
            setImageDataBounds(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        }
    }

    public synchronized Rectangle2D getImageDataBounds() {
        return imageDataBounds != null ? (Rectangle2D) imageDataBounds.clone() : null;
    }

    public synchronized void setImageDataBounds(Rectangle2D imageDataBounds) {
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

    @Override
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
        super.draw(g2, area, anchor, parentState, info);

        if (image != null) {
            final Rectangle2D dataArea = info.getDataArea();
            final double minX = imageDataBounds.getX();
            final double minY = imageDataBounds.getY();
            final double sx = image.getWidth() / imageDataBounds.getWidth();
            final double sy = image.getHeight() / imageDataBounds.getHeight();
            final int dx1 = (int) dataArea.getMinX();
            final int dy1 = (int) dataArea.getMinY();
            final int dx2 = (int) dataArea.getMaxX();
            final int dy2 = (int) dataArea.getMaxY();
            final ValueAxis xAxis = getDomainAxis();
            final ValueAxis yAxis = getRangeAxis();
            final int sx1 = crop(sx * (xAxis.getLowerBound() - minX), 0, image.getWidth() - 1);
            final int sx2 = crop(sx * (xAxis.getUpperBound() - minX), 0, image.getWidth() - 1);
            final int sy2 = crop(image.getHeight() - 1 - sy * (yAxis.getLowerBound() - minY), 0, image.getHeight() - 1);
            final int sy1 = crop(image.getHeight() - 1 - sy * (yAxis.getUpperBound() - minY), 0, image.getHeight() - 1);
            g2.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
            drawOutline(g2, info.getDataArea()); // outline is overwritten by image, so redraw
        }
    }

    private static int crop(double v, int i1, int i2) {
        int i = (int) Math.round(v);
        if (i < i1) return i1;
        if (i > i2) return i2;
        return i;
    }
}
