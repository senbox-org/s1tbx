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
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.util.math.IndexValidator;
import org.esa.snap.util.math.Range;

import java.io.IOException;

public class CursorGraph extends TimeSeriesGraph {

    public CursorGraph() {
    }

    @Override
    public String getYName() {
        return "Cursor";
    }

    @Override
    public void readValues(final ImageLayer imageLayer, final GeoPos geoPos, final int level) {
        resetData();
        for (Band band : selectedBands) {
            final int index = getTimeIndex(band);
            if (index >= 0) {
                final PixelPos pix = band.getGeoCoding().getPixelPos(geoPos, null);
                dataPoints[index] = getPixelDouble(band, (int) pix.getX(), (int) pix.getY());

                if (dataPoints[index] == band.getNoDataValue()) {
                    dataPoints[index] = Double.NaN;
                }
            }
        }
        Range.computeRangeDouble(dataPoints, IndexValidator.TRUE, dataPointRange, ProgressMonitor.NULL);
        // no invalidate() call here, SpectrumDiagram does this
    }

    public static double getPixelDouble(final RasterDataNode raster, final int x, final int y) {

        if (raster.hasRasterData()) {
            if (raster.isPixelValid(x, y)) {
                if (raster.isFloatingPointType()) {
                    return raster.getPixelDouble(x, y);
                } else {
                    return raster.getPixelInt(x, y);
                }
            } else {
                return Double.NaN;
            }
        } else {
            try {
                final boolean pixelValid = raster.readValidMask(x, y, 1, 1, new boolean[1])[0];
                if (pixelValid) {
                    if (raster.isFloatingPointType()) {
                        final float[] pixel = raster.readPixels(x, y, 1, 1, new float[1], ProgressMonitor.NULL);
                        return pixel[0];
                    } else {
                        final int[] pixel = raster.readPixels(x, y, 1, 1, new int[1], ProgressMonitor.NULL);
                        return pixel[0];
                    }
                } else {
                    return Double.NaN;
                }
            } catch (IOException e) {
                return Double.NaN;
            }
        }
    }
}