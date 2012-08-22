/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.statistics;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.StxFactory;
import org.esa.beam.framework.datamodel.VirtualBand;

import javax.media.jai.Histogram;
import java.awt.Color;

/**
 * @author Thomas Storm
 */
class PrecisePercentile {

    final double percentile;
    final double maxError;

    static PrecisePercentile createPrecisePercentile(RasterDataNode raster, Histogram histogram, double percentile) {
        return createPrecisePercentile(new RasterDataNode[]{raster}, histogram, percentile);
    }

    static PrecisePercentile createPrecisePercentile(RasterDataNode[] raster, Histogram histogram, double percentile) {
        return new PrecisePercentile(raster, histogram, percentile);
    }

    private PrecisePercentile(RasterDataNode[] raster, Histogram histogram, double percentile) {
        final double imprecisePercentile = histogram.getPTileThreshold(percentile)[0];
        int binIdx = 0;
        int count = 0;
        int percentileBinIndex = -1;
        while (histogram.getBinLowValue(0, binIdx) < imprecisePercentile) {
            percentileBinIndex = binIdx;
            count += histogram.getBinSize(0, binIdx);
            binIdx++;
        }
        percentileBinIndex++;

        final Histogram higherResolutionHistogram = createHigherResolutionHistogram(raster, histogram, percentileBinIndex);

        maxError = getBinWidth(higherResolutionHistogram);

        double temporaryPercentile = Double.NaN;
        int i = 0;
        while ((double) count / (double) histogram.getTotals()[0] < percentile) {
            count += higherResolutionHistogram.getBinSize(0, i);
            temporaryPercentile = higherResolutionHistogram.getBinLowValue(0, i);
            i++;
        }
        this.percentile = temporaryPercentile;
    }

    private Histogram createHigherResolutionHistogram(RasterDataNode[] raster, Histogram histogram, int binContainingPercentile) {
        final Mask[] masks = createMasks(raster, histogram, binContainingPercentile);
        return new StxFactory()
                .withHistogramBinCount(histogram.getNumBins()[0])
                .withMinimum(histogram.getBinLowValue(0, binContainingPercentile))
                .withMaximum(histogram.getBinLowValue(0, binContainingPercentile) + getBinWidth(histogram))
                .create(ProgressMonitor.NULL, masks, raster)
                .getHistogram();
    }

    private Mask[] createMasks(RasterDataNode[] rasters, Histogram histogram, int binContainingPercentile) {
        final Mask[] masks = new Mask[rasters.length];
        for (int i = 0; i < rasters.length; i++) {
            final RasterDataNode raster = rasters[i];
            final String rasterName;
            if (raster instanceof VirtualBand) {
                rasterName = ((VirtualBand) raster).getExpression();
            } else {
                rasterName = raster.getName();
            }
            final String expression = String.format("%s >= %s and %s <= %s",
                                                    rasterName,
                                                    histogram.getBinLowValue(0, binContainingPercentile),
                                                    rasterName,
                                                    histogram.getBinLowValue(0, binContainingPercentile) +
                                                    getBinWidth(histogram));
            masks[i] = raster.getProduct().addMask("filterMask",
                                                   expression,
                                                   "mask containing only values within provided intervals",
                                                   Color.RED,
                                                   0.5);
        }
        return masks;
    }

    private double getBinWidth(Histogram histogram) {
        return (histogram.getHighValue(0) - histogram.getLowValue(0)) / histogram.getNumBins(0);
    }
}
