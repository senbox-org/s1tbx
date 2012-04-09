package org.esa.beam.visat.toolviews.stat;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Norman Fomferra
 */
public class XYPlotMarker implements ChartMouseListener {
    private final ChartPanel chartPanel;
    private XYDataset xyDataset;
    private int seriesIndex;
    private ShapeOverlay overlay;
    private Paint fillPaint;
    private Paint outlinePaint;
    private Stroke outlineStroke;
    private double markerSize;

    public XYPlotMarker(ChartPanel chartPanel) {
        this.chartPanel = chartPanel;
        fillPaint = new Color(255, 255, 255, 127);
        outlinePaint = new Color(50, 50, 50, 200);
        outlineStroke = new BasicStroke(1.5F);
        markerSize = 20;
    }

    public double getMarkerSize() {
        return markerSize;
    }

    public void setMarkerSize(double markerSize) {
        this.markerSize = markerSize;
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setFillPaint(Paint fillPaint) {
        this.fillPaint = fillPaint;
    }

    public Paint getOutlinePaint() {
        return outlinePaint;
    }

    public void setOutlinePaint(Paint outlinePaint) {
        this.outlinePaint = outlinePaint;
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        if (overlay != null) {
            chartPanel.removeOverlay(overlay);
        }

        xyDataset = null;
        seriesIndex = -1;

        XYPlot plot = chartPanel.getChart().getXYPlot();

        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        Point point = event.getTrigger().getPoint();

        if (!dataArea.contains(point)) {
             return;
        }
        PlotOrientation orientation = plot.getOrientation();
        RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(
                plot.getDomainAxisLocation(), orientation);
        RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(
                plot.getRangeAxisLocation(), orientation);

        double mx = plot.getDomainAxis().java2DToValue(point.x, dataArea, domainEdge);
        double my = plot.getRangeAxis().java2DToValue(point.y, dataArea, rangeEdge);

        int datasetCount = chartPanel.getChart().getXYPlot().getDatasetCount();
        double minDist = Double.POSITIVE_INFINITY;
        for (int datasetIndex = 0; datasetIndex < datasetCount; datasetIndex++) {
            XYDataset xyDataset = chartPanel.getChart().getXYPlot().getDataset(datasetIndex);
            int seriesCount = xyDataset.getSeriesCount();
            for (int seriesIndex = 0; seriesIndex < seriesCount; seriesIndex++) {
                int itemCount = xyDataset.getItemCount(seriesIndex);
                for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                    double x = xyDataset.getXValue(seriesIndex, itemIndex);
                    double y = xyDataset.getYValue(seriesIndex, itemIndex);
                    double dist = (x - mx) * (x - mx) + (y - my) * (y - my);
                    if (dist < minDist) {
                        minDist = dist;
                        this.xyDataset = xyDataset;
                        this.seriesIndex = seriesIndex;
                    }
                }
            }
        }

        if (xyDataset != null) {
            overlay = new ShapeOverlay();
            chartPanel.addOverlay(overlay);
            updatePoint(event);
        }
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {
        updatePoint(event);

    }

    private void updatePoint(ChartMouseEvent event) {
        if (xyDataset == null) {
            return;
        }

        XYPlot plot = chartPanel.getChart().getXYPlot();

        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        PlotOrientation orientation = plot.getOrientation();
        RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(
                plot.getDomainAxisLocation(), orientation);
        RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(
                plot.getRangeAxisLocation(), orientation);

        double x2d = event.getTrigger().getPoint().x;
        double x = plot.getDomainAxis().java2DToValue(x2d, dataArea, domainEdge);

        int itemCount = xyDataset.getItemCount(seriesIndex);
        for (int i = 0; i < itemCount - 1; i++) {
            double x1 = xyDataset.getXValue(seriesIndex, i);
            double x2 = xyDataset.getXValue(seriesIndex, i + 1);
            if (x >= x1 && x <= x2) {
                double y1 = xyDataset.getYValue(seriesIndex, i);
                double y2 = xyDataset.getYValue(seriesIndex, i + 1);
                double y = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
                double y2d = plot.getRangeAxis().valueToJava2D(y, dataArea, rangeEdge);
                overlay.setPoint(new Point2D.Double(x2d, y2d));
                break;
            }
        }
    }

    private class ShapeOverlay extends AbstractOverlay implements Overlay {

        Point2D point;

        public void setPoint(Point2D point) {
            if (this.point == null || !this.point.equals(point)) {
                this.point = point;
                fireOverlayChanged();
            }
        }

        @Override
        public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
            if (point != null) {
                Shape oldClip = g2.getClip();
                Rectangle2D dataArea = chartPanel.getScreenDataArea();
                g2.setClip(dataArea);

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Ellipse2D.Double ellipse = new Ellipse2D.Double(point.getX() - 0.5 * markerSize,
                                                                point.getY() - 0.5 * markerSize,
                                                                markerSize,
                                                                markerSize);
                g2.setPaint(fillPaint);
                g2.fill(ellipse);
                g2.setStroke(outlineStroke);
                g2.setPaint(outlinePaint);
                g2.draw(ellipse);

                Rectangle2D box = new Rectangle2D.Double(dataArea.getX() + 5, dataArea.getY() + 5, 100, 52);
                g2.setPaint(fillPaint);
                g2.fill(box);
                g2.setStroke(outlineStroke);
                g2.setPaint(outlinePaint);
                g2.draw(box);

                g2.drawString(String.format("x = %.3f", point.getX()), (int) (dataArea.getX() + 5 + 5), (int) (dataArea.getY() + 5 + 20));
                g2.drawString(String.format("y = %.3f", point.getY()), (int) (dataArea.getX() + 5 + 5), (int) (dataArea.getY() + 5 + 40));

                g2.setClip(oldClip);
            }
        }

    }

}
