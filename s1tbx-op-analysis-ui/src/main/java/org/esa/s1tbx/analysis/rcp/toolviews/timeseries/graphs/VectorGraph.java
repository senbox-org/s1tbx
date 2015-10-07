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
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.TimeSeriesTimes;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.VectorDataNode;
import org.esa.snap.jai.ImageManager;
import org.esa.snap.util.math.IndexValidator;
import org.esa.snap.util.math.Range;
import org.geotools.geometry.jts.ReferencedEnvelope;

import javax.media.jai.PlanarImage;
import java.awt.image.Raster;

public class VectorGraph extends TimeSeriesGraph {

    public enum TYPE {AVERAGE, STD_DEV}

    private VectorDataNode vectorNode;
    private final TYPE type;
    private boolean dataComputed = false;

    public VectorGraph(final VectorDataNode vectorNode, final TYPE type) {
        this.vectorNode = vectorNode;
        this.type = type;
    }

    @Override
    public String getXName() {
        return "Time";
    }

    @Override
    public String getYName() {
        return vectorNode.getName();
    }

    @Override
    public void setBands(final TimeSeriesTimes tsTimes, final Band[] selBands) {
        super.setBands(tsTimes, selBands);

        dataComputed = false;
    }

    private void computeData() {
        resetData();
        for (Band band : selectedBands) {
            final int index = getTimeIndex(band);
            if (index >= 0) {
                dataPoints[index] = processVector(band);
                if (dataPoints[index] == band.getNoDataValue()) {
                    dataPoints[index] = Double.NaN;
                }
            }
        }
        dataComputed = true;
    }

    @Override
    public void readValues(final ImageLayer imageLayer, final GeoPos geoPos, final int level) {
        if (!dataComputed) {
            computeData();
        }
        Range.computeRangeDouble(dataPoints, IndexValidator.TRUE, dataPointRange, ProgressMonitor.NULL);
        // no invalidate() call here, SpectrumDiagram does this
    }

    private double processVector(final Band band) {
        final GeoCoding bandGC = band.getGeoCoding();
        final ReferencedEnvelope env = vectorNode.getEnvelope();
        final String envCode = env.getCoordinateReferenceSystem().getName().getCode();

        final PixelPos topLeft, bottomRight;
        if (envCode.startsWith("Image CS based on")) {
            topLeft = new PixelPos((float) env.getMinX(), (float) env.getMinY());
            bottomRight = new PixelPos((float) env.getMaxX(), (float) env.getMaxY());
        } else {
            final GeoPos geo1 = new GeoPos((float) env.getMinY(), (float) env.getMinX());
            final GeoPos geo2 = new GeoPos((float) env.getMaxY(), (float) env.getMaxX());

            topLeft = bandGC.getPixelPos(geo1, null);
            bottomRight = bandGC.getPixelPos(geo2, null);
        }

        final int minX = (int) Math.min(topLeft.getX(), bottomRight.getX());
        final int maxX = (int) Math.max(topLeft.getX(), bottomRight.getX());
        final int minY = (int) Math.min(topLeft.getY(), bottomRight.getY());
        final int maxY = (int) Math.max(topLeft.getY(), bottomRight.getY());

        final double noDataValue = band.getNoDataValue();
        final PlanarImage image = ImageManager.getInstance().getSourceImage(band, 0);

        boolean isStdDev = false;
        double[] samples = null;
        if (type == TYPE.STD_DEV) {
            isStdDev = true;
            samples = new double[Math.max(1, (maxX + 1 - minX) * (maxY + 1 - minY))];
        }

        double sum = 0;
        int cnt = 0;
        for (int x = minX; x <= maxX; ++x) {
            final int tileX = image.XToTileX(x);
            for (int y = minY; y <= maxY; ++y) {
                final int tileY = image.YToTileY(y);
                final Raster data = image.getTile(tileX, tileY);
                if (data == null) {
                    continue;
                }

                final double sample;
                if (band.getDataType() == ProductData.TYPE_INT8) {
                    sample = (byte) data.getSample(x, y, 0);
                } else if (band.getDataType() == ProductData.TYPE_UINT32) {
                    sample = data.getSample(x, y, 0) & 0xFFFFFFFFL;
                } else {
                    sample = data.getSampleDouble(x, y, 0);
                }

                if (!Double.isNaN(sample) && sample != noDataValue) {
                    sum += sample;
                    if (isStdDev) {
                        samples[cnt] = sample;
                    }
                    ++cnt;
                }
            }
        }

        final double mean = sum / (long) cnt;

        if (isStdDev) {
            double sqrSum = 0.0;
            for (double sample : samples) {
                double delta = sample - mean;
                sqrSum += delta * delta;
            }
            return Math.sqrt(sqrSum / (double) cnt);
        }
        return mean;
    }

    @Override
    public void dispose() {
        vectorNode = null;
        super.dispose();
    }
}