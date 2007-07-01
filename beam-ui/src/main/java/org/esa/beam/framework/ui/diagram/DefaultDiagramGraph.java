package org.esa.beam.framework.ui.diagram;

import org.esa.beam.util.math.Range;
import org.esa.beam.util.math.IndexValidator;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;

public class DefaultDiagramGraph implements DiagramGraph {
    private String xName;
    private double[] xValues;
    private String yName;
    private double[] yValues;

    private Range xRange;
    private Range yRange;

    private DiagramGraphStyle style;

    public DefaultDiagramGraph() {
    }

    public DefaultDiagramGraph(String xName,
                               double[] xValues,
                               String yName,
                               double[] yValues) {
        Assert.notNull(yName, "yName");
        setXName(xName);
        setYName(yName);
        setXYValues(xValues, yValues);
    }

    public String getXName() {
        return xName;
    }

    public void setXName(String xName) {
        Assert.notNull(xName, "xName");
        this.xName = xName;
    }

    public String getYName() {
        return yName;
    }

    public void setYName(String yName) {
        Assert.notNull(yName, "yName");
        this.yName = yName;
    }

    public double[] getXValues() {
        return xValues;
    }

    public double[] getYValues() {
        return yValues;
    }

    public void setXYValues(double[] xValues, double[] yValues) {
        Assert.notNull(xValues, "xValues");
        Assert.notNull(yValues, "yValues");
        Assert.argument(xValues.length > 1, "xValues.length > 1");
        Assert.argument(xValues.length == yValues.length, "xValues.length == yValues.length");
        this.xValues = xValues;
        this.yValues = yValues;
        this.xRange = Range.computeRangeDouble(xValues, IndexValidator.TRUE, null, ProgressMonitor.NULL);
        this.yRange = Range.computeRangeDouble(yValues, IndexValidator.TRUE, null, ProgressMonitor.NULL);
    }

    public int getNumValues() {
        return xValues.length;
    }

    public double getXValueAt(int index) {
        return xValues[index];
    }

    public double getYValueAt(int index) {
        return yValues[index];
    }

    public double getXMin() {
        return xRange.getMin();
    }

    public double getXMax() {
        return xRange.getMax();
    }

    public double getYMin() {
        return yRange.getMin();
    }

    public double getYMax() {
        return yRange.getMax();
    }

    public void setStyle(DiagramGraphStyle style) {
        this.style = style;
    }

    public DiagramGraphStyle getStyle() {
        if (style == null) {
            style = new DefaultDiagramGraphStyle();
        }
        return style;
    }
}
