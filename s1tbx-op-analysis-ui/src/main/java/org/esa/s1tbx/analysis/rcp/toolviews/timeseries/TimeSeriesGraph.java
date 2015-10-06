/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.util.math.IndexValidator;
import org.esa.snap.core.util.math.Range;
import org.esa.snap.ui.diagram.AbstractDiagramGraph;

import java.util.HashMap;
import java.util.Map;


public abstract class TimeSeriesGraph extends AbstractDiagramGraph {

    protected TimeSeriesTimes times;
    protected Band[] selectedBands;
    protected double[] dataPoints;
    protected double[] timeData;
    protected final Range dataPointRange = new Range();
    protected final Range timeRange = new Range();
    private final Map<Band, Integer> timeBandMap = new HashMap<>(10);

    public TimeSeriesGraph() {
    }

    @Override
    public String getXName() {
        return "Time";
    }

    @Override
    public String getYName() {
        return "Cursor";
    }

    @Override
    public final int getNumValues() {
        return dataPoints.length;
    }

    @Override
    public final double getXValueAt(int index) {
        return timeData[index];
    }

    @Override
    public double getYValueAt(int index) {
        return dataPoints[index];
    }

    @Override
    public final double getXMin() {
        return timeRange.getMin();
    }

    @Override
    public final double getXMax() {
        return timeRange.getMax();
    }

    @Override
    public final double getYMin() {
        return dataPointRange.getMin();
    }

    @Override
    public final double getYMax() {
        return dataPointRange.getMax();
    }

    protected final boolean isSelected(final Band band) {
        for (Band b : selectedBands) {
            if (b == band)
                return true;
        }
        return false;
    }

    protected final void resetData() {
        for (int i = 0; i < dataPoints.length; ++i) {
            dataPoints[i] = Double.NaN;
        }
    }

    public final int getTimeIndex(final Band band) {
        final Integer index = timeBandMap.get(band);
        return index == null ? times.getIndex(band) : index;
    }

    public void setBands(final TimeSeriesTimes tsTimes, final Band[] selBands) {
        this.times = tsTimes;
        this.selectedBands = selBands.clone();
        final int numTimes = this.times.length();
        if (timeData == null || timeData.length != numTimes) {
            timeData = new double[numTimes];
        }
        if (dataPoints == null || dataPoints.length != numTimes) {
            dataPoints = new double[numTimes];
        }
        for (int i = 0; i < numTimes; i++) {
            timeData[i] = times.getTimeAt(i).getMJD();
            dataPoints[i] = 0.0f;
        }
        timeBandMap.clear();
        for (Band band : selectedBands) {
            timeBandMap.put(band, times.getIndex(band));
        }
        Range.computeRangeDouble(timeData, IndexValidator.TRUE, timeRange, ProgressMonitor.NULL);
        Range.computeRangeDouble(dataPoints, IndexValidator.TRUE, dataPointRange, ProgressMonitor.NULL);
    }

    public abstract void readValues(final ImageLayer imageLayer, final GeoPos geoPos, final int level);

    @Override
    public void dispose() {
        times = null;
        selectedBands = null;
        dataPoints = null;
        timeData = null;
        super.dispose();
    }
}