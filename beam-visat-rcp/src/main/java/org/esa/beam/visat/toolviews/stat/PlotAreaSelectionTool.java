package org.esa.beam.visat.toolviews.stat;

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
import java.awt.geom.Rectangle2D;

/**
 * @author Norman Fomferra
 */
public class PlotAreaSelectionTool extends MouseAdapter {
    private final ChartPanel chartPanel;
    private final Action action;

    private ShapeOverlay overlay;
    private Point point1;
    private Point point2;
    private double triggerDistance;
    private Color fillPaint;

    public PlotAreaSelectionTool(ChartPanel chartPanel, Action action) {
        this.chartPanel = chartPanel;
        this.action = action;
        triggerDistance = 4;
        fillPaint = new Color(0, 0, 255, 50);
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
        this.fillPaint = fillPaint;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        point1 = event.getPoint();
        point2 = point1;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (overlay != null) {
            chartPanel.removeOverlay(overlay);

            XYPlot plot = chartPanel.getChart().getXYPlot();

            Rectangle2D dataArea = chartPanel.getScreenDataArea();
            PlotOrientation orientation = plot.getOrientation();
            RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(
                    plot.getDomainAxisLocation(), orientation);
            RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(
                    plot.getRangeAxisLocation(), orientation);

            double x1 = plot.getDomainAxis().java2DToValue(point1.x, dataArea, domainEdge);
            double y1 = plot.getRangeAxis().java2DToValue(point1.y, dataArea, rangeEdge);
            double x2 = plot.getDomainAxis().java2DToValue(point2.x, dataArea, domainEdge);
            double y2 = plot.getRangeAxis().java2DToValue(point2.y, dataArea, rangeEdge);
            double dx = Math.abs(x2 - x1);
            double dy = Math.abs(y2 - y1);

            action.ellipseSelected(x1, y1, dx, dy);
            System.out.printf(String.format("x1 = %s, y1 = %s,\ndx = %s, dy = %s\n",
                                            x1, y1, dx, dy));

            overlay = null;
            point1 = null;
            point2 = null;
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        point2 = event.getPoint();
        if (overlay == null && point1 != null) {
            if (Point.distanceSq(point1.x, point1.y, point2.x, point2.y) >= triggerDistance * triggerDistance) {
                overlay = new ShapeOverlay();
                chartPanel.addOverlay(overlay);
            }
        }
        if (overlay != null) {
            overlay.fireOverlayChanged();
        }
    }

    public interface Action {
        void ellipseSelected(double x0, double y0, double rx, double ry);
    }

    private class ShapeOverlay extends AbstractOverlay implements Overlay {
        @Override
        public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
            if (point1 != null && point2 != null) {
                double dx = Math.abs(point2.x - point1.x);
                double dy = Math.abs(point2.y - point1.y);
                if (dx < 5) {
                    dx = 5;
                }
                if (dy < 5) {
                    dy = 5;
                }

                Shape oldClip = g2.getClip();
                g2.setClip(chartPanel.getScreenDataArea());
                Ellipse2D.Double ellipse = new Ellipse2D.Double(point1.x - dx, point1.y - dy,
                                                                2 * dx, 2 * dy);
                g2.setPaint(fillPaint);
                g2.fill(ellipse);

                g2.setClip(oldClip);
            }
        }
    }
}
