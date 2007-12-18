package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.Assert;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * An X/Y plot that uses a buffered image to display its data.
 * @author Norman Fomferra
 */
class XYImagePlot extends XYPlot {

    private BufferedImage image;
    private Rectangle2D imageDataBounds;

    public XYImagePlot() {
        super(null, new NumberAxis("X"), new NumberAxis("Y"), new XYLineAndShapeRenderer(false, false));
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
            final int dx1 = (int) dataArea.getMinX();
            final int dy1 = (int) dataArea.getMinY();
            final int dx2 = (int) dataArea.getMaxX();
            final int dy2 = (int) dataArea.getMaxY();

            final Rectangle rectangle = getImageSourceArea();
            final int sx1 = rectangle.x;
            final int sy1 = rectangle.y;
            final int sx2 = sx1 + rectangle.width - 1;
            final int sy2 = sy1 + rectangle.height - 1;

            g2.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);

            drawOutline(g2, info.getDataArea()); // outline is overwritten by image, so redraw
        }
    }

    Rectangle getImageSourceArea() {
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
        if (i < i1) return i1;
        if (i > i2) return i2;
        return i;
    }
}
