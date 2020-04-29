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
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;

/**
 * Interface for Speckle Filters
 */
public interface SpeckleFilter {

    void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm);

    double[][] performFiltering(final int x0, final int y0, final int w, final int h, final String[] srcBandNames);

    /**
     * Get source tile rectangle.
     *
     * @param x0                X coordinate of the upper left corner point of the target tile rectangle.
     * @param y0                Y coordinate of the upper left corner point of the target tile rectangle.
     * @param w                 The width of the target tile rectangle.
     * @param h                 The height of the target tile rectangle.
     * @param halfSizeX         Half of the sliding window width.
     * @param halfSizeY         Half of the sliding window height.
     * @param sourceImageWidth  Source image width.
     * @param sourceImageHeight Source image height.
     * @return The source tile rectangle.
     */
    default Rectangle getSourceTileRectangle(final int x0, final int y0, final int w, final int h,
                                             final int halfSizeX, final int halfSizeY,
                                             final int sourceImageWidth, final int sourceImageHeight) {
        final int sx0 = Math.max(0, x0 - halfSizeX);
        final int sy0 = Math.max(0, y0 - halfSizeY);
        final int sw = Math.min(x0 + w + halfSizeX, sourceImageWidth) - sx0;
        final int sh = Math.min(y0 + h + halfSizeY, sourceImageHeight) - sy0;
        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     * Get pixel values in a filter size rectangular region centered at the given pixel.
     *
     * @param tx                X coordinate of a given pixel.
     * @param ty                Y coordinate of a given pixel.
     * @param srcData1          The source ProductData for 1st band.
     * @param srcData2          The source ProductData for 2nd band.
     * @param srcIndex          The source tile index.
     * @param noDataValue       Place holder for no data value.
     * @param isComplex         True if it has i and q, otherwise false.
     * @param windowSizeX       The sliding window width.
     * @param windowSizeY       The sliding window height.
     * @param sourceImageWidth  Source image width.
     * @param sourceImageHeight Source image height.
     * @param neighborValues    Array holding the pixel values.
     * @return The number of valid samples.
     * @throws OperatorException If an error occurs in obtaining the pixel values.
     */
    default int getNeighborValues(final int tx, final int ty, final ProductData srcData1, final ProductData srcData2,
                                  final TileIndex srcIndex, final double noDataValue, final boolean isComplex,
                                  final int windowSizeX, final int windowSizeY, final int sourceImageWidth,
                                  final int sourceImageHeight, final double[] neighborValues) {

        final int halfSizeX = windowSizeX / 2;
        final int halfSizeY = windowSizeY / 2;
        final int minX = tx - halfSizeX;
        final int maxX = minX + windowSizeX - 1;
        final int minY = ty - halfSizeY;
        final int maxY = minY + windowSizeY - 1;
        final int height = sourceImageHeight - 1;
        final int width = sourceImageWidth - 1;

        int numValidSamples = 0;
        int k = 0;
        if (isComplex) {

            for (int y = minY; y <= maxY; y++) {
                if (y < 0 || y > height) {
                    for (int x = minX; x <= maxX; x++) {
                        neighborValues[k++] = noDataValue;
                    }
                } else {
                    srcIndex.calculateStride(y);
                    for (int x = minX; x <= maxX; x++) {
                        if (x < 0 || x > width) {
                            neighborValues[k++] = noDataValue;
                        } else {
                            final int idx = srcIndex.getIndex(x);
                            final double I = srcData1.getElemDoubleAt(idx);
                            final double Q = srcData2.getElemDoubleAt(idx);
                            if (Double.compare(I, noDataValue) != 0 && Double.compare(Q, noDataValue) != 0) {
                                neighborValues[k++] = I * I + Q * Q;
                                numValidSamples++;
                            } else {
                                neighborValues[k++] = noDataValue;
                            }
                        }
                    }
                }
            }

        } else {

            for (int y = minY; y <= maxY; y++) {
                if (y < 0 || y > height) {
                    for (int x = minX; x <= maxX; x++) {
                        neighborValues[k++] = noDataValue;
                    }
                } else {
                    srcIndex.calculateStride(y);
                    for (int x = minX; x <= maxX; x++) {
                        if (x < 0 || x > width) {
                            neighborValues[k++] = noDataValue;
                        } else {
                            final double v = srcData1.getElemDoubleAt(srcIndex.getIndex(x));
                            neighborValues[k++] = v;
                            if (Double.compare(v, noDataValue) != 0) {
                                numValidSamples++;
                            }
                        }
                    }
                }
            }
        }

        return numValidSamples;
    }

    /**
     * Get the mean value of pixel intensities in a given rectangular region.
     *
     * @param neighborValues The pixel values in the given rectangular region.
     * @param numSamples     The number of samples.
     * @param noDataValue    Place holder for no data value.
     * @return mean The mean value.
     */
    default double getMeanValue(final double[] neighborValues, final int numSamples, final double noDataValue) {

        double mean = 0.0;
        for (double v : neighborValues) {
            if (v != noDataValue) {
                mean += v;
            }
        }
        mean /= numSamples;

        return mean;
    }

    /**
     * Get the variance of pixel intensities in a given rectangular region.
     *
     * @param neighborValues The pixel values in the given rectangular region.
     * @param numSamples     The number of samples.
     * @param mean           the mean of neighborValues.
     * @param noDataValue    Place holder for no data value.
     * @return var The variance value.
     * @throws OperatorException If an error occurs in computation of the variance.
     */
    default double getVarianceValue(
            final double[] neighborValues, final int numSamples, final double mean, final double noDataValue) {

        double var = 0.0;
        if (numSamples > 1) {

            for (double v : neighborValues) {
                if (v != noDataValue) {
                    final double diff = v - mean;
                    var += diff * diff;
                }
            }
            var /= (numSamples - 1);
        }

        return var;
    }

    /**
     * Compute the equivalent number of looks.
     *
     * @param srcData1    The source ProductData for the 1st band.
     * @param srcData2    The source ProductData for the 2nd band.
     * @param noDataValue The place holder for no data.
     * @param bandUnit    Unit for 1st band.
     * @param srcIndex    The source tile index.
     * @param x0          X coordinate of the upper left corner point of the target tile rectangle.
     * @param y0          Y coordinate of the upper left corner point of the target tile rectangle.
     * @param w           The width of the target tile rectangle.
     * @param h           The height of the target tile rectangle.
     * @return The equivalent number of looks.
     */
    default double computeEquivalentNumberOfLooks(
            final ProductData srcData1, final ProductData srcData2, final double noDataValue,
            final Unit.UnitType bandUnit, final TileIndex srcIndex, final int x0, final int y0,
            final int w, final int h) {

        double enl = 1.0;
        double sum = 0;
        double sum2 = 0;
        double sum4 = 0;
        int numSamples = 0;

        if (bandUnit != null && (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY)) {

            for (int y = y0; y < y0 + h; y++) {
                srcIndex.calculateStride(y);
                for (int x = x0; x < x0 + w; x++) {
                    final int idx = srcIndex.getIndex(x);

                    final double i = srcData1.getElemDoubleAt(idx);
                    final double q = srcData2.getElemDoubleAt(idx);
                    if (i != noDataValue && q != noDataValue) {
                        double v = i * i + q * q;
                        sum += v;
                        sum2 += v * v;
                        numSamples++;
                    }
                }
            }

            if (sum != 0.0 && sum2 > 0.0) {
                final double m = sum / numSamples;
                final double m2 = sum2 / numSamples;
                final double mm = m * m;
                enl = mm / (m2 - mm);
            }

        } else if (bandUnit != null && bandUnit == Unit.UnitType.INTENSITY) {

            for (int y = y0; y < y0 + h; y++) {
                srcIndex.calculateStride(y);
                for (int x = x0; x < x0 + w; x++) {
                    final int idx = srcIndex.getIndex(x);

                    final double v = srcData1.getElemDoubleAt(idx);
                    if (v != noDataValue) {
                        sum += v;
                        sum2 += v * v;
                        numSamples++;
                    }
                }
            }

            if (sum != 0.0 && sum2 > 0.0) {
                final double m = sum / numSamples;
                final double m2 = sum2 / numSamples;
                final double mm = m * m;
                enl = mm / (m2 - mm);
            }

        } else {

            for (int y = y0; y < y0 + h; y++) {
                srcIndex.calculateStride(y);
                for (int x = x0; x < x0 + w; x++) {
                    final int idx = srcIndex.getIndex(x);

                    final double v = srcData1.getElemDoubleAt(idx);
                    if (v != noDataValue) {
                        final double v2 = v * v;
                        sum2 += v2;
                        sum4 += v2 * v2;
                        numSamples++;
                    }
                }
            }

            if (sum2 > 0.0 && sum4 > 0.0) {
                final double m2 = sum2 / numSamples;
                final double m4 = sum4 / numSamples;
                final double m2m2 = m2 * m2;
                enl = m2m2 / (m4 - m2m2);
            }
        }

        return enl;
    }

    /**
     * Get pixel intensities in a filter size rectangular region centered at the given pixel.
     *
     * @param x                   X coordinate of the given pixel.
     * @param y                   Y coordinate of the given pixel.
     * @param srcData1            The data buffer of the first band.
     * @param srcData2            The data buffer of the second band.
     * @param srcIndex            The source tile index.
     * @param noDataValue         The place holder for no data.
     * @param bandUnit            Unit for the 1st band.
     * @param sourceTileRectangle The source tile rectangle.
     * @param filterSizeX         Sliding window width.
     * @param filterSizeY         Sliding window height.
     * @param neighborPixelValues 2-D array holding the pixel values.
     * @return The number of valid pixels.
     * @throws OperatorException If an error occurs in obtaining the pixel values.
     */
    default int getNeighborValuesWithoutBorderExt(final int x, final int y, final ProductData srcData1,
                                                  final ProductData srcData2, final TileIndex srcIndex,
                                                  final double noDataValue, final Unit.UnitType bandUnit,
                                                  final Rectangle sourceTileRectangle,
                                                  final int filterSizeX, final int filterSizeY,
                                                  final double[][] neighborPixelValues) {

        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sw = sourceTileRectangle.width;
        final int sh = sourceTileRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;
        final int halfSizeX = filterSizeX / 2;
        final int halfSizeY = filterSizeY / 2;
        int numSamples = 0;
        if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {

            for (int j = 0; j < filterSizeY; ++j) {
                final int yj = y - halfSizeY + j;
                if (yj < sy0 || yj >= maxY) {
                    for (int i = 0; i < filterSizeX; ++i) {
                        neighborPixelValues[j][i] = noDataValue;
                    }
                    continue;
                }

                srcIndex.calculateStride(yj);
                for (int i = 0; i < filterSizeX; ++i) {
                    final int xi = x - halfSizeX + i;
                    if (xi < sx0 || xi >= maxX) {
                        neighborPixelValues[j][i] = noDataValue;
                    } else {
                        final int idx = srcIndex.getIndex(xi);
                        final double I = srcData1.getElemDoubleAt(idx);
                        final double Q = srcData2.getElemDoubleAt(idx);
                        if (I != noDataValue && Q != noDataValue) {
                            neighborPixelValues[j][i] = I * I + Q * Q;
                            numSamples++;
                        } else {
                            neighborPixelValues[j][i] = noDataValue;
                        }
                    }
                }
            }

        } else {

            for (int j = 0; j < filterSizeY; ++j) {
                final int yj = y - halfSizeY + j;
                if (yj < sy0 || yj >= maxY) {
                    for (int i = 0; i < filterSizeX; ++i) {
                        neighborPixelValues[j][i] = noDataValue;
                    }
                    continue;
                }

                srcIndex.calculateStride(yj);
                for (int i = 0; i < filterSizeX; ++i) {
                    final int xi = x - halfSizeX + i;
                    if (xi < sx0 || xi >= maxX) {
                        neighborPixelValues[j][i] = noDataValue;
                    } else {
                        final int idx = srcIndex.getIndex(xi);
                        neighborPixelValues[j][i] = srcData1.getElemDoubleAt(idx);
                        if (neighborPixelValues[j][i] != noDataValue) {
                            numSamples++;
                        }
                    }
                }
            }
        }

        return numSamples;
    }

    default double computeMMSEWeight(final double[] dataArray, final double sigmaVSqr) {

        final double meanY = getMeanValue(dataArray);
        final double varY = getVarianceValue(dataArray, meanY);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX = (varY - meanY * meanY * sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }
        return varX / varY;
    }

    default double getMeanValue(final double[] neighborValues) {

        double mean = 0.0;
        for (double neighborValue : neighborValues) {
            mean += neighborValue;
        }
        mean /= neighborValues.length;

        return mean;
    }

    default  double getVarianceValue(final double[] neighborValues, final double mean) {

        double var = 0.0;
        if (neighborValues.length > 1) {

            for (double neighborValue : neighborValues) {
                final double diff = neighborValue - mean;
                var += diff * diff;
            }
            var /= (neighborValues.length - 1);
        }

        return var;
    }

}
