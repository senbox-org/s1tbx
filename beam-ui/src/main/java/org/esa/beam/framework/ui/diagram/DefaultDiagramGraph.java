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

package org.esa.beam.framework.ui.diagram;

import org.esa.beam.util.math.Range;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.ObjectUtils;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;

public class DefaultDiagramGraph extends AbstractDiagramGraph  {

    private String xName;
    private double[] xValues;
    private String yName;
    private double[] yValues;
    private Range xRange;
    private Range yRange;

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
        if (!ObjectUtils.equalObjects(this.xName,  xName)) {
            this.xName = xName;
            invalidate();
        }
    }

    public String getYName() {
        return yName;
    }

    public void setYName(String yName) {
        Assert.notNull(yName, "yName");
        if (!ObjectUtils.equalObjects(this.yName,  yName)) {
            this.yName = yName;
            invalidate();
        }
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
        if (!ObjectUtils.equalObjects(this.xValues,  xValues) || !ObjectUtils.equalObjects(this.yValues,  yValues)) {
            this.xValues = xValues;
            this.yValues = yValues;
            this.xRange = Range.computeRangeDouble(xValues, IndexValidator.TRUE, null, ProgressMonitor.NULL);
            this.yRange = Range.computeRangeDouble(yValues, IndexValidator.TRUE, null, ProgressMonitor.NULL);
            invalidate();
        }
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

    @Override
    public void dispose() {
        xValues = null;
        yValues = null;
        super.dispose();
    }
}
