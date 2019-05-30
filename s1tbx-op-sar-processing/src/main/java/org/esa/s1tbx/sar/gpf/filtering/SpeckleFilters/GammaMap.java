/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.filtering.SpeckleFilters;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * GammaMap Speckle Filter
 */
public class GammaMap implements SpeckleFilter {

    private final Operator operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private final int windowSizeX;
    private final int windowSizeY;
    private final int halfWindowSizeX;
    private final int halfWindowSizeY;
    private final int sourceImageWidth;
    private final int sourceImageHeight;
    private Map<String, String[]> targetBandNameToSourceBandName;
    final boolean estimateENL;
    final double enl;

    public GammaMap(final Operator op, final Product srcProduct, final Product trgProduct, final int windowSizeX,
                    final int windowSizeY, final Map<String, String[]> targetBandNameToSourceBandName,
                    final boolean estimateENL, final double enl) {

        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.windowSizeX = windowSizeX;
        this.windowSizeY = windowSizeY;
        this.halfWindowSizeX = windowSizeX / 2;
        this.halfWindowSizeY = windowSizeY / 2;
        this.sourceImageWidth = srcProduct.getSceneRasterWidth();
        this.sourceImageHeight = srcProduct.getSceneRasterHeight();
        this.targetBandNameToSourceBandName = targetBandNameToSourceBandName;
        this.estimateENL = estimateENL;
        this.enl = enl;
    }

    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;
            final int xMax = x0 + w;
            final int yMax = y0 + h;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            final double[][] filteredTile = performFiltering(x0, y0, w, h, srcBandNames);

            final ProductData tgtData = targetTile.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(targetTile);
            for (int y = y0; y < yMax; ++y) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < xMax; ++x) {
                    tgtData.setElemDoubleAt(tgtIndex.getIndex(x), filteredTile[yy][x - x0]);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("GammaMap", e);
        } finally {
            pm.done();
        }
    }

    public double[][] performFiltering(
            final int x0, final int y0, final int w, final int h, final String[] srcBandNames) {

        final double[][] filteredTile = new double[h][w];

        final Rectangle sourceTileRectangle = getSourceTileRectangle(
                x0, y0, w, h, halfWindowSizeX, halfWindowSizeY, sourceImageWidth, sourceImageHeight);

        Band sourceBand1 = null;
        Band sourceBand2 = null;
        Tile sourceTile1 = null;
        Tile sourceTile2 = null;
        ProductData sourceData1 = null;
        ProductData sourceData2 = null;
        if (srcBandNames.length == 1) {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceTile1 = operator.getSourceTile(sourceBand1, sourceTileRectangle);
            sourceData1 = sourceTile1.getDataBuffer();
        } else {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
            sourceTile1 = operator.getSourceTile(sourceBand1, sourceTileRectangle);
            sourceTile2 = operator.getSourceTile(sourceBand2, sourceTileRectangle);
            sourceData1 = sourceTile1.getDataBuffer();
            sourceData2 = sourceTile2.getDataBuffer();
        }
        final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
        final double noDataValue = sourceBand1.getNoDataValue();
        final TileIndex srcIndex = new TileIndex(sourceTile1);

        double n;
        if (estimateENL) {
            n = computeEquivalentNumberOfLooks(sourceData1, sourceData2, noDataValue, bandUnit,
                    srcIndex, x0, y0, w, h);
        } else {
            n = enl;
        }
        final double cu = 1.0 / Math.sqrt(n);
        final double cu2 = cu * cu;

        final double[] neighborValues = new double[windowSizeX * windowSizeY];
        final boolean isComplex = bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        for (int y = y0; y < yMax; ++y) {
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int xx = x - x0;

                final int numSamples = getNeighborValues(
                        x, y, sourceData1, sourceData2, srcIndex, noDataValue, isComplex,
                        windowSizeX, windowSizeY, sourceImageWidth, sourceImageHeight, neighborValues);

                if (numSamples > 0) {
                    filteredTile[yy][xx] = getGammaMapValue(neighborValues, numSamples, noDataValue, cu, cu2, n);
                } else {
                    filteredTile[yy][xx] = noDataValue;
                }
            }
        }

        return filteredTile;
    }

    /**
     * Get the Gamma filtered pixel intensity for pixels in a given rectangular region.
     *
     * @param neighborValues Array holding the pixel values.
     * @param numSamples     The number of samples.
     * @param noDataValue    Place holder for no data value.
     * @return val The Gamma filtered value.
     * @throws OperatorException If an error occurs in computation of the Gamma filtered value.
     */
    private double getGammaMapValue(final double[] neighborValues, final int numSamples, final double noDataValue,
                                    final double cu, final double cu2, final double enl) {

        final double mean = getMeanValue(neighborValues, numSamples, noDataValue);
        if (mean <= Double.MIN_VALUE) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, numSamples, mean, noDataValue);
        if (var <= Double.MIN_VALUE) {
            return mean;
        }

        final double ci = Math.sqrt(var) / mean;
        if (ci <= cu) {
            return mean;
        }

        final double cp = neighborValues[neighborValues.length / 2];

        if (cu < ci) {
            final double cmax = Math.sqrt(2) * cu;
            if (ci < cmax) {
                final double alpha = (1 + cu2) / (ci * ci - cu2);
                final double b = alpha - enl - 1;
                final double d = mean * mean * b * b + 4 * alpha * enl * mean * cp;
                return (b * mean + Math.sqrt(d)) / (2 * alpha);
            }
        }

        return cp;
    }
}
