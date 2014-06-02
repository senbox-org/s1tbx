package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.Assert;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import static java.lang.Math.abs;
import static java.lang.Math.min;

/**
 * @author Norman Fomferra
 */
public class PlotAreaSelectionTool extends MouseAdapter {
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
        void areaSelected(AreaType areaType, Shape shape);
    }

    private final ChartPanel chartPanel;
    private final Action action;

    private Point point1;
    private Point point2;
    private SelectedArea selectedArea;
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
        if (!isButton1(event)) {
            return;
        }
        point1 = event.getPoint();
        point2 = null;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (!isButton1(event)) {
            return;
        }
        if (selectedArea == null) {
            return;
        }
        // Make sure, action is only triggered if a new area has been selected
        if (point1 == null || point2 == null) {
            return;
        }
        action.areaSelected(areaType, selectedArea.getShape());
        // Ready for a new area to be selected, but the existing area remains visible
        point1 = null;
        point2 = null;
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (point1 == null) {
            return;
        }

        if (point2 == null) {
            // first drag event after mousePressed -->
            // then we must check against triggerDistance
            Point p2 = event.getPoint();
            if (Point.distanceSq(point1.getX(), point1.getY(), p2.getX(), p2.getY()) >= triggerDistance * triggerDistance) {
                point2 = p2;
                updateAnnotation();
            }
        } else {
            // already dragging, just update
            point2 = event.getPoint();
            updateAnnotation();
        }
    }

    private void updateAnnotation() {
        removeAnnotation();
        addAnnotation();
    }

    private void addAnnotation() {
        selectedArea = new SelectedArea(createShape(), fillPaint);
        chartPanel.getChart().getXYPlot().addAnnotation(selectedArea);
    }

    public void removeAnnotation() {
        if (selectedArea != null) {
            chartPanel.getChart().getXYPlot().removeAnnotation(selectedArea);
            selectedArea = null;
        }
    }

    private boolean isButton1(MouseEvent event) {
        return event.getButton() == MouseEvent.BUTTON1;
    }

    private Shape createShape() {
        XYPlot plot = chartPanel.getChart().getXYPlot();

        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        PlotOrientation orientation = plot.getOrientation();
        RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(plot.getDomainAxisLocation(), orientation);
        RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(plot.getRangeAxisLocation(), orientation);

        double vx1 = areaType == AreaType.Y_RANGE ? dataArea.getX() : point1.x;
        double vy1 = areaType == AreaType.X_RANGE ? dataArea.getY() : point1.y;

        double vx2 = areaType == AreaType.Y_RANGE ? dataArea.getX() + dataArea.getWidth() : point2.x;
        double vy2 = areaType == AreaType.X_RANGE ? dataArea.getY() + dataArea.getHeight() : point2.y;

        double x1 = plot.getDomainAxis().java2DToValue(vx1, dataArea, domainEdge);
        double x2 = plot.getDomainAxis().java2DToValue(vx2, dataArea, domainEdge);
        double y1 = plot.getRangeAxis().java2DToValue(vy1, dataArea, rangeEdge);
        double y2 = plot.getRangeAxis().java2DToValue(vy2, dataArea, rangeEdge);

        double dx = abs(x2 - x1);
        double dy = abs(y2 - y1);

        final Shape shape;
        if (areaType == AreaType.ELLIPSE) {
            shape = new Ellipse2D.Double(x1 - dx, y1 - dy, 2.0 * dx, 2.0 * dy);
        } else if (areaType == AreaType.RECTANGLE) {
            shape = new Rectangle2D.Double(x1 - dx, y1 - dy, 2.0 * dx, 2.0 * dy);
        } else if (areaType == AreaType.X_RANGE || areaType == AreaType.Y_RANGE) {
            shape = new Rectangle2D.Double(min(x1, x2), min(y1, y2), dx, dy);
        } else {
            throw new IllegalStateException("areaType = " + areaType);
        }

        return shape;
    }


    private static class SelectedArea extends XYShapeAnnotation  {
        private final Shape shape;
        private SelectedArea(Shape shape, Paint fillPaint) {
            super(shape, null, null, fillPaint);
            this.shape = shape;
        }

        // Base class does not off this method :-(
        public Shape getShape() {
            return shape;
        }
    }

}
