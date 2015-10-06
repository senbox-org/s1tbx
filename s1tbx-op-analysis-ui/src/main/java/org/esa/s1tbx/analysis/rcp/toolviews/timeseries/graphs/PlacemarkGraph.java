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
import org.esa.snap.core.util.math.IndexValidator;
import org.esa.snap.core.util.math.Range;

public class PlacemarkGraph extends TimeSeriesGraph {

    private Placemark placemark = null;
    private final String graphName;

    public PlacemarkGraph(final Placemark placemark, String graphName) {
        this.placemark = placemark;
        this.graphName = graphName;
    }

    public Placemark getPlacemark() {
        return placemark;
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
        if (placemark != null) {
            for (Band band : selectedBands) {
                final int index = getTimeIndex(band);
                if (index >= 0) {
                    final PixelPos pix = band.getGeoCoding().getPixelPos(placemark.getGeoPos(), null);

                    /*final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(band);
                    final AffineTransform i2mTransform = multiLevelModel.getImageToModelTransform(0);
                    final AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(level);
                    final Point2D modelPixel = i2mTransform.transform(placemark.getPixelPos(), null);
                    final Point2D imagePixel = m2iTransform.transform(modelPixel, null);
                    final int pixX = (int) Math.floor(imagePixel.getX());
                    final int pixY = (int) Math.floor(imagePixel.getY());   */

                    dataPoints[index] = ProductUtils.getGeophysicalSampleAsLong(band, (int) pix.getX(), (int) pix.getY(), level);
                    if (dataPoints[index] == band.getNoDataValue()) {
                        dataPoints[index] = Double.NaN;
                    }
                }
            }
        }
        Range.computeRangeDouble(dataPoints, IndexValidator.TRUE, dataPointRange, ProgressMonitor.NULL);
        // no invalidate() call here, SpectrumDiagram does this
    }

    @Override
    public void dispose() {
        placemark = null;
        super.dispose();
    }
}