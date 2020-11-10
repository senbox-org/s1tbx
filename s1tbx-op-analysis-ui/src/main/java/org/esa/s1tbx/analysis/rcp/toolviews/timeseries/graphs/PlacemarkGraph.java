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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries.graphs;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.TimeSeriesGraph;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.ThreadExecutor;
import org.esa.snap.core.util.ThreadRunnable;
import org.esa.snap.core.util.math.IndexValidator;
import org.esa.snap.core.util.math.Range;

public class PlacemarkGraph extends TimeSeriesGraph {

    private Placemark placemark;
    private final String graphName;

    public PlacemarkGraph(final Placemark placemark, final String graphName) {
        this.placemark = placemark;
        this.graphName = graphName;
    }

    @Override
    public String getXName() {
        return "Time";
    }

    @Override
    public String getYName() {
        return graphName + ' ' + placemark.getLabel();
    }

    @Override
    public void readValues(final ImageLayer imageLayer, final GeoPos geoPos, final int level) {
        resetData();

        try {
            final ThreadExecutor executor = new ThreadExecutor();
            if (placemark != null) {
                for (Band band : selectedBands) {
                    final int index = getTimeIndex(band);
                    if (index >= 0) {
                        final ThreadRunnable runnable = new ThreadRunnable() {
                            @Override
                            public void process() {
                                final PixelPos pix = band.getGeoCoding().getPixelPos(placemark.getGeoPos(), null);

                                dataPoints[index] = ProductUtils.getGeophysicalSampleAsDouble(band, (int) pix.getX(), (int) pix.getY(), 0);
                                if (dataPoints[index] == band.getNoDataValue()) {
                                    dataPoints[index] = Double.NaN;
                                }
                            }
                        };
                        executor.execute(runnable);
                    }
                }
            }
            executor.complete();
        } catch (Exception e) {
            SystemUtils.LOG.severe("PlacemarkGraph unable to read values " + e.getMessage());
        }

        if(dataPoints != null) {
            Range.computeRangeDouble(dataPoints, IndexValidator.TRUE, dataPointRange, ProgressMonitor.NULL);
        }
    }

    @Override
    public void dispose() {
        placemark = null;
        super.dispose();
    }
}