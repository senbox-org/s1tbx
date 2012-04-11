package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.Assert;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Norman Fomferra
 */
class PlotAreaSelectionTool extends MouseAdapter {
    public enum AreaType {
        /**
         * Shape is an instance of {@link Rectangle2D}, only X-coordinates are valid.
         */
        X_RANGE,
        /**
         * Shape is an instance of {@link Rectangle2D}, only Y-coordinates are valid.
         */
        Y_RANGE,
        /**
         * Shape is an instance of {@link Rectangle2D}.
         */
        RECTANGLE,
        /**
         * Shape is an instance of {@link Ellipse2D}.
         */
        ELLIPSE,
    }

    public interface Action {
        void areaSelected(AreaType areaType, double x0, double y0, double dx, double dy);
    }

    private final ChartPanel chartPanel;
    private final Action action;

    private ShapeOverlay overlay;
    private Point2D point1;
    private Point2D point2;
    private double triggerDistance;
    private Color fillPaint;
    private AreaType areaType;

    public PlotAreaSelectionTool(ChartPanel chartPanel, Action action) {
        this.chartPanel = chartPanel;
        this.action = action;
        triggerDistance = 4;
        fillPaint = new Color(0, 0, 255, 50);
        areaType = AreaType.ELLIPSE;
    }

    public void install() {
        chartPanel.addMouseListener(this);
        chartPanel.addMouseMotionListener(this);
        chartPanel.setMouseZoomable(false);
    }

    public void uninstall() {
        chartPanel.removeMouseListener(this);
        chartPanel.removeMouseMotionListener(this);
        chartPanel.setMouseZoomable(true);
    }

    public void removeOverlay() {
        if (overlay != null) {
            chartPanel.removeOverlay(overlay);
            overlay = null;
        }
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public void setAreaType(AreaType areaType) {
        Assert.notNull(areaType, "areaType");
        this.areaType = areaType;
    }

    public double getTriggerDistance() {
        return triggerDistance;
    }

    public void setTriggerDistance(double triggerDistance) {
        this.triggerDistance = triggerDistance;
    }

    public Color getFillPaint() {
        return fillPaint;
    }

    public void setFillPaint(Color fillPaint) {
        Assert.notNull(fillPaint, "fillPaint");
        this.fillPaint = fillPaint;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        removeOverlay();
        point1 = getPoint1(event);
        point2 = getPoint2(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (overlay != null) {

            XYPlot plot = chartPanel.getChart().getXYPlot();

            Rectangle2D dataArea = chartPanel.getScreenDataArea();
            PlotOrientation orientation = plot.getOrientation();
            RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(plot.getDomainAxisLocation(), orientation);
            RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(plot.getRangeAxisLocation(), orientation);

            double x1 = plot.getDomainAxis().java2DToValue(point1.getX(), dataArea, domainEdge);
            double y1 = plot.getRangeAxis().java2DToValue(point1.getY(), dataArea, rangeEdge);
            double x2 = plot.getDomainAxis().java2DToValue(point2.getX(), dataArea, domainEdge);
            double y2 = plot.getRangeAxis().java2DToValue(point2.getY(), dataArea, rangeEdge);

            double x0 = Math.min(x1, x2);
            double y0 = Math.min(y1, y2);
            double dx = Math.abs(x2 - x1);
            double dy = Math.abs(y2 - y1);

            action.areaSelected(areaType, x0, y0, dx, dy);

            point1 = null;
            point2 = null;
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        point2 = getPoint2(event);
        if (overlay == null && point1 != null) {
            if (Point.distanceSq(point1.getX(), point1.getY(), point2.getX(), point2.getY()) >= triggerDistance * triggerDistance) {
                overlay = new ShapeOverlay();
                chartPanel.addOverlay(overlay);
            }
        }
        if (overlay != null) {
            overlay.fireOverlayChanged();
        }
    }

    private Point2D.Double getPoint1(MouseEvent event) {
        final Point point = event.getPoint();
        final Rectangle2D dataArea = chartPanel.getScreenDataArea();
        final double x = areaType == AreaType.Y_RANGE ? dataArea.getX() : point.x;
        final double y = areaType == AreaType.X_RANGE ? dataArea.getY() : point.y;
        return new Point2D.Double(x, y);
    }

    private Point2D.Double getPoint2(MouseEvent event) {
        final Point point = event.getPoint();
        final Rectangle2D dataArea = chartPanel.getScreenDataArea();
        final double x = areaType == AreaType.Y_RANGE ? dataArea.getX() + dataArea.getWidth() : point.x;
        final double y = areaType == AreaType.X_RANGE ? dataArea.getY() + dataArea.getHeight() : point.y;
        return new Point2D.Double(x, y);
    }

    private class ShapeOverlay extends AbstractOverlay implements Overlay {
        @Override
        public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
            if (point1 != null && point2 != null) {
                double x0 = Math.min(point2.getX(), point1.getX());
                double y0 = Math.min(point2.getY(), point1.getY());
                double dx = Math.abs(point2.getX() - point1.getX());
                double dy = Math.abs(point2.getY() - point1.getY());
                final Shape shape;
                if (areaType == AreaType.ELLIPSE) {
                    shape = new Ellipse2D.Double(x0 - dx, y0 - dy, 2 * dx, 2 * dy);
                } else if (areaType == AreaType.RECTANGLE) {
                    shape = new Rectangle2D.Double(x0 - dx, y0 - dy, 2 * dx, 2 * dy);
                } else if (areaType == AreaType.X_RANGE || areaType == AreaType.Y_RANGE) {
                    shape = new Rectangle2D.Double(x0, y0, dx, dy);
                } else {
                    throw new IllegalStateException("areaType = " + areaType);
                }

                Shape oldClip = g2.getClip();
                g2.setClip(chartPanel.getScreenDataArea());
                g2.setPaint(fillPaint);
                g2.fill(shape);
                g2.setClip(oldClip);
            }
        }
    }
}
