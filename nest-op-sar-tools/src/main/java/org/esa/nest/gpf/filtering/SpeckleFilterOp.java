/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.filtering;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies a Speckle Filter to the data
 */
@OperatorMetadata(alias="Speckle-Filter",
        category = "SAR Tools\\Speckle Filtering",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Speckle Reduction")
public class SpeckleFilterOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band", 
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {MEAN_SPECKLE_FILTER, MEDIAN_SPECKLE_FILTER, FROST_SPECKLE_FILTER,
            GAMMA_MAP_SPECKLE_FILTER, LEE_SPECKLE_FILTER, LEE_REFINED_FILTER}, defaultValue = LEE_REFINED_FILTER,
            label="Filter")
    private String filter;

    @Parameter(description = "The kernel x dimension", interval = "(1, 100]", defaultValue = "3", label="Size X")
    private int filterSizeX = 3;

    @Parameter(description = "The kernel y dimension", interval = "(1, 100]", defaultValue = "3", label="Size Y")
    private int filterSizeY = 3;

    @Parameter(description = "The damping factor (Frost filter only)", interval = "(0, 100]", defaultValue = "2",
                label="Frost Damping Factor")
    private int dampingFactor = 2;

    @Parameter(description = "The edge threshold (Refined Lee filter only)", interval = "(0, *)", defaultValue = "5000",
                label="Edge detection threshold")
    private double edgeThreshold = 5000.0;

    @Parameter(defaultValue="false", label="Estimate Eqivalent Number of Looks")
    private boolean estimateENL = true;

    @Parameter(description = "The number of looks", interval = "(0, *)", defaultValue = "1.0",
                label="Number of looks")
    private double enl = 1.0;

    static final String MEAN_SPECKLE_FILTER = "Mean";
    static final String MEDIAN_SPECKLE_FILTER = "Median";
    static final String FROST_SPECKLE_FILTER = "Frost";
    static final String GAMMA_MAP_SPECKLE_FILTER = "Gamma Map";
    static final String LEE_SPECKLE_FILTER = "Lee";
    static final String LEE_REFINED_FILTER = "Refined Lee";

    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();
    private int halfSizeX;
    private int halfSizeY;
    private int sourceImageWidth;
    private int sourceImageHeight;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SpeckleFilterOp() {
    }

    /**
     * Set speckle filter. This function is used by unit test only.
     * @param s The filter name.
     */
    public void SetFilter(String s) {

        if (s.equals(MEAN_SPECKLE_FILTER) ||
            s.equals(MEDIAN_SPECKLE_FILTER) ||
            s.equals(FROST_SPECKLE_FILTER) ||
            s.equals(GAMMA_MAP_SPECKLE_FILTER) ||
            s.equals(LEE_SPECKLE_FILTER) ||
            s.equals(LEE_REFINED_FILTER)) {
                filter = s;
        } else {
            throw new OperatorException(s + " is an invalid filter name.");
        }
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            if(filter.equals(LEE_REFINED_FILTER)) {
                filterSizeX = 7;
                filterSizeY = 7;
            }

            halfSizeX = filterSizeX / 2;
            halfSizeY = filterSizeY / 2;

            createTargetProduct();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        OperatorUtils.addSelectedBands(
                sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, true, true);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override

    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int tw = targetTileRectangle.width;
            final int th = targetTileRectangle.height;
            final ProductData trgData = targetTile.getDataBuffer();
            //System.out.println("x0 = " + tx0 + ", y0 = " + ty0 + ", w = " + tw + ", h = " + th);

            final Rectangle sourceTileRectangle = getSourceTileRectangle(tx0, ty0, tw, th);
            Tile sourceTile1 = null;
            Tile sourceTile2 = null;
            ProductData sourceData1 = null;
            ProductData sourceData2 = null;
            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            Band sourceBand1;
            if (srcBandNames.length == 1) {
                sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
                sourceTile1 = getSourceTile(sourceBand1, sourceTileRectangle);
                sourceData1 = sourceTile1.getDataBuffer();
            } else {
                sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
                Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
                sourceTile1 = getSourceTile(sourceBand1, sourceTileRectangle);
                sourceTile2 = getSourceTile(sourceBand2, sourceTileRectangle);
                sourceData1 = sourceTile1.getDataBuffer();
                sourceData2 = sourceTile2.getDataBuffer();
            }
            final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
            final double noDataValue = sourceBand1.getNoDataValue();

            final TileIndex trgIndex = new TileIndex(targetTile);
            final TileIndex srcIndex = new TileIndex(sourceTile1);

            final double[] neighborValues = new double[filterSizeX*filterSizeY];


            double cu, cu2, n;

            switch (filter) {
                case MEAN_SPECKLE_FILTER:

                    computeMean(sourceData1, sourceData2, trgData, noDataValue, bandUnit, neighborValues,
                                srcIndex, trgIndex, tx0, ty0, tw, th);

                    break;
                case MEDIAN_SPECKLE_FILTER:

                    computeMedian(sourceData1, sourceData2, trgData, noDataValue, bandUnit, neighborValues,
                                  srcIndex, trgIndex, tx0, ty0, tw, th);

                    break;
                case FROST_SPECKLE_FILTER:

                    computeFrost(sourceData1, sourceData2, trgData, noDataValue, bandUnit, neighborValues,
                                 srcIndex, trgIndex, tx0, ty0, tw, th);

                    break;
                case GAMMA_MAP_SPECKLE_FILTER:

                    if (estimateENL) {
                        n = computeEquivalentNumberOfLooks(sourceData1, sourceData2, noDataValue, bandUnit,
                                                           srcIndex, tx0, ty0, tw, th);
                    } else {
                        n = enl;
                    }
                    cu = 1.0 / Math.sqrt(n);
                    cu2 = cu * cu;

                    computeGammaMap(sourceData1, sourceData2, trgData, noDataValue, bandUnit, neighborValues,
                                    srcIndex, trgIndex, tx0, ty0, tw, th, cu, cu2, n);

                    break;
                case LEE_SPECKLE_FILTER:

                    if (estimateENL) {
                        n = computeEquivalentNumberOfLooks(sourceData1, sourceData2, noDataValue, bandUnit,
                                                           srcIndex, tx0, ty0, tw, th);
                    } else {
                        n = enl;
                    }
                    cu = 1.0 / Math.sqrt(n);
                    cu2 = cu * cu;

                    computeLee(sourceData1, sourceData2, trgData, noDataValue, bandUnit, neighborValues,
                               srcIndex, trgIndex, tx0, ty0, tw, th, cu, cu2);

                    break;
                case LEE_REFINED_FILTER:

                    computeRefinedLee(sourceData1, sourceData2, trgData, noDataValue, bandUnit,
                                      srcIndex, trgIndex, tx0, ty0, tw, th, sourceTileRectangle);
                    break;
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Get source tile rectangle.
     * @param x0 X coordinate of the upper left corner point of the target tile rectangle.
     * @param y0 Y coordinate of the upper left corner point of the target tile rectangle.
     * @param w The width of the target tile rectangle.
     * @param h The height of the target tile rectangle.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(int x0, int y0, int w, int h) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;

        if (x0 >= halfSizeX) {
            sx0 -= halfSizeX;
            sw += halfSizeX;
        }

        if (y0 >= halfSizeY) {
            sy0 -= halfSizeY;
            sh += halfSizeY;
        }

        if (x0 + w + halfSizeX <= sourceImageWidth) {
            sw += halfSizeX;
        }

        if (y0 + h + halfSizeY <= sourceImageHeight) {
            sh += halfSizeY;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     * Filter the given tile of image with Mean filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData.
     * @param noDataValue the place holder for no data.
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill.
     * @param srcIndex The source tile index.
     * @param trgIndex The target tile index.
     * @param tx0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param ty0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param tw Width for the target_Tile_Rectangle.
     * @param th Height for the target_Tile_Rectangle.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeMean(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                             final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                             final TileIndex srcIndex, final TileIndex trgIndex,
                             final int tx0, final int ty0, final int tw, final int th) {

        final int maxY = ty0 + th;
        final int maxX = tx0 + tw;
        for (int y = ty0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = tx0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, unit, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getMeanValue(neighborValues, numSamples));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Filter the given tile of image with Median filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param noDataValue the place holder for no data
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill
     * @param srcIndex The source tile index.
     * @param trgIndex The target tile index.
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Height for the target_Tile_Rectangle.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeMedian(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                               final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                               final TileIndex srcIndex, final TileIndex trgIndex,
                               final int x0, final int y0, final int w, final int h) {

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, unit, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getMedianValue(neighborValues, numSamples));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Filter the given tile of image with Frost filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData.
     * @param noDataValue the place holder for no data.
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill.
     * @param srcIndex The source tile index.
     * @param trgIndex The target tile index.
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Height for the target_Tile_Rectangle.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeFrost(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                              final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                              final TileIndex srcIndex, final TileIndex trgIndex,
                              final int x0, final int y0, final int w, final int h) {

        final double[] mask = new double[filterSizeX*filterSizeY];
        getFrostMask(mask);

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, unit, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getFrostValue(neighborValues, numSamples, mask));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Filter the given tile of image with Gamma filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param noDataValue the place holder for no data.
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill.
     * @param srcIndex The source tile index.
     * @param trgIndex The target tile index.
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Height for the target_Tile_Rectangle.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeGammaMap(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                                 final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                                 final TileIndex srcIndex, final TileIndex trgIndex,
                                 final int x0, final int y0, final int w, final int h,
                                 final double cu, final double cu2, final double enl) {

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, unit, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getGammaMapValue(neighborValues, numSamples, cu, cu2, enl));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Filter the given tile of image with Lee filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param noDataValue the place holder for no data.
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill.
     * @param srcIndex The source tile index.
     * @param trgIndex The target tile index.
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Height for the target_Tile_Rectangle.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeLee(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                            final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                            final TileIndex srcIndex, final TileIndex trgIndex,
                            final int x0, final int y0, final int w, final int h,
                            final double cu, final double cu2) {

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, unit, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getLeeValue(neighborValues, numSamples, cu, cu2));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Get pixel values in a filter size rectangular region centered at the given pixel.
     * @param tx X coordinate of a given pixel.
     * @param ty Y coordinate of a given pixel.
     * @param srcData1 The source ProductData for 1st band.
     * @param srcData2 The source ProductData for 2nd band.
     * @param srcIndex The source tile index.
     * @param noDataValue the place holder for no data
     * @param bandUnit Unit for the 1st band.
     * @param neighborValues Array holding the pixel values.
     * @return The number of valid samples.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in obtaining the pixel values.
     */
    private int getNeighborValues(final int tx, final int ty, final ProductData srcData1, final ProductData srcData2,
                                  final TileIndex srcIndex, final double noDataValue, final Unit.UnitType bandUnit,
                                  final double[] neighborValues) {

        final int x0 = Math.max(tx - halfSizeX, 0);
        final int y0 = Math.max(ty - halfSizeY, 0);
        final int w  = Math.min(tx + halfSizeX, sourceImageWidth - 1) - x0 + 1;
        final int h  = Math.min(ty + halfSizeY, sourceImageHeight - 1) - y0 + 1;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        int numSamples = 0;
        if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {

            for (int y = y0; y < yMax; y++) {
                srcIndex.calculateStride(y);
                for (int x = x0; x < xMax; x++) {
                    final int idx = srcIndex.getIndex(x);
                    final double I = srcData1.getElemDoubleAt(idx);
                    final double Q = srcData2.getElemDoubleAt(idx);
                    if (I != noDataValue && Q != noDataValue) {
                        neighborValues[numSamples++] = I*I + Q*Q;
                    }
                }
            }

        } else {

            for (int y = y0; y < yMax; y++) {
                srcIndex.calculateStride(y);
                for (int x = x0; x < xMax; x++) {
                    final int idx = srcIndex.getIndex(x);
                    final double v = srcData1.getElemDoubleAt(idx);
                    if (v != noDataValue) {
                        neighborValues[numSamples++] = v;
                    }
                }
            }
        }

        return numSamples;
    }

    /**
     * Get the mean value of pixel intensities in a given rectangular region.
     * @param neighborValues The pixel values in the given rectangular region.
     * @param numSamples The number of samples.
     * @return mean The mean value.
     */
    private static double getMeanValue(final double[] neighborValues, final int numSamples) {

        double mean = 0.0;
        for (int i = 0; i < numSamples; i++) {
            mean += neighborValues[i];
        }
        mean /= numSamples;

        return mean;
    }

    /**
     * Get the variance of pixel intensities in a given rectangular region.
     * @param neighborValues The pixel values in the given rectangular region.
     * @param numSamples The number of samples.
     * @param mean the mean of neighborValues.
     * @return var The variance value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the variance.
     */
    private static double getVarianceValue(final double[] neighborValues, final int numSamples, final double mean) {

        double var = 0.0;
        if (numSamples > 1) {

            for (int i = 0; i < numSamples; i++) {
                final double diff = neighborValues[i] - mean;
                var += diff * diff;
            }
            var /= (numSamples - 1);
        }

        return var;
    }

    /**
     * Get the median value of pixel intensities in a given rectangular region.
     * @param neighborValues Array holding pixel values.
     * @param numSamples The number of samples.
     * @return median The median value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the median value.
     */
    private static double getMedianValue(final double[] neighborValues, final int numSamples) {

        Arrays.sort(neighborValues, 0, numSamples);

        // Then get the median value
        return neighborValues[(numSamples / 2)];
    }

    /**
     * Get Frost mask for given Frost filter size.
     * @param mask Array holding Frost filter mask values.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the Frost mask.
     */
    private void getFrostMask(final double[] mask) {

        for (int i = 0; i < filterSizeX; i++) {

            final int s = i*filterSizeY;
            final int dr = Math.abs(i - halfSizeX);

            for (int j = 0; j < filterSizeY; j++) {
                mask[j + s] = Math.max(dr, Math.abs(j - halfSizeY));
            }
        }
    }

    /**
     * Get the Frost filtered pixel intensity for pixels in a given rectangular region.
     * @param neighborValues Array holding the pixel values.
     * @param numSamples The number of samples.
     * @param mask Array holding Frost filter mask values.
     * @return val The Frost filtered value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the Frost filtered value.
     */
    private double getFrostValue(final double[] neighborValues, final int numSamples, final double[] mask) {

        final double mean = getMeanValue(neighborValues, numSamples);
        if (mean <= Double.MIN_VALUE) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, numSamples, mean);
        if (var <= Double.MIN_VALUE) {
            return mean;
        }

        final double k = dampingFactor * var / (mean*mean);

        double sum = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < neighborValues.length; i++) {
            final double weight = FastMath.exp(-k * mask[i]);
            sum += weight * neighborValues[i];
            totalWeight += weight;
        }
        return sum / totalWeight;
    }

    /**
     * Get the Gamma filtered pixel intensity for pixels in a given rectangular region.
     * @param neighborValues Array holding the pixel values.
     * @param numSamples The number of samples.
     * @return val The Gamma filtered value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the Gamma filtered value.
     */
    private double getGammaMapValue(
            final double[] neighborValues, final int numSamples, final double cu, final double cu2, final double enl) {

        final double mean = getMeanValue(neighborValues, numSamples);
        if (mean <= Double.MIN_VALUE) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, numSamples, mean);
        if (var <= Double.MIN_VALUE) {
            return mean;
        }

        final double ci = Math.sqrt(var) / mean;
        if (ci <= cu) {
            return mean;
        }

        final double cp = neighborValues[(numSamples/2)];

        if (cu < ci) {
            final double cmax = Math.sqrt(2)*cu;
            if(ci < cmax) {
                final double alpha = (1 + cu2) / (ci*ci - cu2);
                final double b = alpha - enl - 1;
                final double d = mean*mean*b*b + 4*alpha*enl*mean*cp;
                return (b*mean + Math.sqrt(d)) / (2*alpha);
            }
        }

        return cp;
    }

    /**
     * Get the Lee filtered pixel intensity for pixels in a given rectangular region.
     * @param neighborValues Array holding the pixel values.
     * @param numSamples The number of samples.
     * @return val The Lee filtered value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the Lee filtered value.
     */
    private double getLeeValue(final double[] neighborValues, final int numSamples, final double cu, final double cu2) {

        final double mean = getMeanValue(neighborValues, numSamples);
        if (Double.compare(mean, Double.MIN_VALUE) <= 0) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, numSamples, mean);
        if (Double.compare(var, Double.MIN_VALUE) <= 0) {
            return mean;
        }

        final double ci = Math.sqrt(var) / mean;
        if (ci < cu) {
            return mean;
        }

        final double cp = neighborValues[(numSamples/2)];
        final double w = 1 - cu2 / (ci*ci);

        return cp*w + mean*(1 - w);
    }

    /**
     * Compute the equivalent number of looks.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param noDataValue The place holder for no data.
     * @param bandUnit Unit for 1st band.
     * @param srcIndex The source tile index.
     * @param x0 X coordinate of the upper left corner point of the target tile rectangle.
     * @param y0 Y coordinate of the upper left corner point of the target tile rectangle.
     * @param w The width of the target tile rectangle.
     * @param h The height of the target tile rectangle.
     * @return The equivalent number of looks.
     */
    private double computeEquivalentNumberOfLooks(
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
                        double v = i*i + q*q;
                        sum += v;
                        sum2 += v*v;
                        numSamples++;
                    }
                }
            }

            if (sum != 0.0 && sum2 > 0.0) {
                final double m = sum / numSamples;
                final double m2 = sum2 / numSamples;
                final double mm = m*m;
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
                        sum2 += v*v;
                        numSamples++;
                    }
                }
            }

            if (sum != 0.0 && sum2 > 0.0) {
                final double m = sum / numSamples;
                final double m2 = sum2 / numSamples;
                final double mm = m*m;
                enl = mm / (m2 - mm);
            }

        } else {

            for (int y = y0; y < y0 + h; y++) {
                srcIndex.calculateStride(y);
                for (int x = x0; x < x0 + w; x++) {
                    final int idx = srcIndex.getIndex(x);

                    final double v = srcData1.getElemDoubleAt(idx);
                    if (v != noDataValue) {
                        final double v2 = v*v;
                        sum2 += v2;
                        sum4 += v2*v2;
                        numSamples++;
                    }
                }
            }

            if (sum2 > 0.0 && sum4 > 0.0) {
                final double m2 = sum2 / numSamples;
                final double m4 = sum4 / numSamples;
                final double m2m2 = m2*m2;
                enl = m2m2 / (m4 - m2m2);
            }
        }

        return enl;
    }

    /**
     * Filter the given tile of image with refined Lee filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param noDataValue The place holder for no data.
     * @param unit Unit for the 1st band.
     * @param srcIndex The source tile index.
     * @param trgIndex The target tile index.
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Height for the target_Tile_Rectangle.
     */
    private void computeRefinedLee(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                                   final double noDataValue, final Unit.UnitType unit,
                                   final TileIndex srcIndex, final TileIndex trgIndex,
                                   final int x0, final int y0, final int w, final int h,
                                   final Rectangle sourceTileRectangle) {

        final double[][] neighborPixelValues = new double[filterSizeY][filterSizeX];
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValuesWithoutBorderExt(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, unit, sourceTileRectangle, neighborPixelValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getRefinedLeeValue(numSamples, noDataValue, neighborPixelValues));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Get pixel intensities in a filter size rectangular region centered at the given pixel.
     * @param x X coordinate of the given pixel.
     * @param y Y coordinate of the given pixel.
     * @param srcData1 The data buffer of the first band.
     * @param srcData2 The data buffer of the second band.
     * @param srcIndex The source tile index.
     * @param noDataValue The place holder for no data.
     * @param bandUnit Unit for the 1st band.
     * @param sourceTileRectangle The source tile rectangle.
     * @param neighborPixelValues 2-D array holding the pixel values.
     * @return The number of valid pixels.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in obtaining the pixel values.
     */
    private int getNeighborValuesWithoutBorderExt(final int x, final int y, final ProductData srcData1,
                                                  final ProductData srcData2, final TileIndex srcIndex,
                                                  final double noDataValue, final Unit.UnitType bandUnit,
                                                  final Rectangle sourceTileRectangle,
                                                  final double[][] neighborPixelValues) {

        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sw = sourceTileRectangle.width;
        final int sh = sourceTileRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;
        int numSamples = 0;
        if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {

            for (int j = 0; j < filterSizeY; ++j) {
                final int yj = y - halfSizeY + j;
                if(yj < sy0 || yj >= maxY) {
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
                            neighborPixelValues[j][i] = I*I + Q*Q;
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
                if(yj < sy0 || yj >= maxY) {
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
                        final double v = srcData1.getElemDoubleAt(idx);
                        if (v != noDataValue) {
                            neighborPixelValues[j][i] = v;
                            numSamples++;
                        } else {
                            neighborPixelValues[j][i] = noDataValue;
                        }
                    }
                }
            }
        }

        return numSamples;
    }

    /**
     * Compute filtered pixel value using refined Lee filter.
     * @param numSamples The number of valid pixel in the neighborhood.
     * @param noDataValue The place holder for no data.
     * @param neighborPixelValues The neighbor pixel values.
     * @return The filtered pixel value.
     */
    private double getRefinedLeeValue(
            final int numSamples, final double noDataValue, final double[][] neighborPixelValues) {

        if (numSamples < filterSizeX*filterSizeY) {
            return computePixelValueUsingLocalStatistics(neighborPixelValues, noDataValue);
        }

        final double var = getLocalVarianceValue(
                getLocalMeanValue(neighborPixelValues, noDataValue), neighborPixelValues, noDataValue);
        if (var < edgeThreshold) {
            return computePixelValueUsingLocalStatistics(neighborPixelValues, noDataValue);
        }

        return computePixelValueUsingEdgeDetection(neighborPixelValues, noDataValue);
    }

    /**
     * Compute filtered pixel value using Local Statistics filter.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue The place holder for no data.
     * @return The filtered pixel value.
     */
    private double computePixelValueUsingLocalStatistics(
            final double[][] neighborPixelValues, final double noDataValue) {

        if (neighborPixelValues[halfSizeY][halfSizeX] == noDataValue) {
            return noDataValue;
        }

        // y is the pixel amplitude or intensity and x is the pixel reflectance before degradation
        final double meanY = getLocalMeanValue(neighborPixelValues, noDataValue);
        if (meanY == noDataValue) {
            return noDataValue;
        }

        final double varY = getLocalVarianceValue(meanY, neighborPixelValues, noDataValue);
        if (varY == 0.0 || varY == noDataValue) {
            return noDataValue;
        }

        final double sigmaV = getLocalNoiseVarianceValue(neighborPixelValues, noDataValue);
        double varX = (varY - meanY*meanY*sigmaV) / (1 + sigmaV);
        if (varX < 0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b*(neighborPixelValues[3][3] - meanY);
    }

    /**
     * Compute filtered pixel value using refined Lee filter.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue The place holder for no data.
     * @return The filtered pixel value.
     */
    private static double computePixelValueUsingEdgeDetection(
            final double[][] neighborPixelValues, final double noDataValue) {

        final double[][] subAreaMeans = new double[3][3];
        computeSubAreaMeans(neighborPixelValues, subAreaMeans);

        final double gradient0 = Math.abs(subAreaMeans[1][0] - subAreaMeans[1][2]);
        final double gradient1 = Math.abs(subAreaMeans[0][2] - subAreaMeans[2][0]);
        final double gradient2 = Math.abs(subAreaMeans[0][1] - subAreaMeans[2][1]);
        final double gradient3 = Math.abs(subAreaMeans[0][0] - subAreaMeans[2][2]);

        int direction = 0;
        double maxGradient = gradient0;
        if (gradient1 > maxGradient) {
            maxGradient = gradient1;
            direction = 1;
        }

        if (gradient2 > maxGradient) {
            maxGradient = gradient2;
            direction = 2;
        }

        if (gradient3 > maxGradient) {
            maxGradient = gradient3;
            direction = 3;
        }

        int d = 0;
        if (direction == 0) {

            if (Math.abs(subAreaMeans[1][0] - subAreaMeans[1][1]) < Math.abs(subAreaMeans[1][1] - subAreaMeans[1][2])) {
                d = 4;
            } else {
                d = 0;
            }

        } else if (direction == 1) {

            if (Math.abs(subAreaMeans[0][2] - subAreaMeans[1][1]) < Math.abs(subAreaMeans[1][1] - subAreaMeans[2][0])) {
                d = 1;
            } else {
                d = 5;
            }

        } else if (direction == 2) {

            if (Math.abs(subAreaMeans[0][1] - subAreaMeans[1][1]) < Math.abs(subAreaMeans[1][1] - subAreaMeans[2][1])) {
                d = 2;
            } else {
                d = 6;
            }

        } else if (direction == 3) {

            if (Math.abs(subAreaMeans[0][0] - subAreaMeans[1][1]) < Math.abs(subAreaMeans[1][1] - subAreaMeans[2][2])) {
                d = 3;
            } else {
                d = 7;
            }
        }

        final double[] pixels = new double[28];
        getNonEdgeAreaPixelValues(neighborPixelValues, d, pixels);

        final double meanY = getMeanValue(pixels, pixels.length);
        final double varY = getVarianceValue(pixels, pixels.length, meanY);
        if (varY == 0.0) {
            return 0.0;
        }
        final double sigmaV = getLocalNoiseVarianceValue(neighborPixelValues, noDataValue);
        double varX = (varY - meanY*meanY*sigmaV) / (1 + sigmaV);
        if (varX < 0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b*(neighborPixelValues[3][3] - meanY);
    }

    /**
     * Compute local mean for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue The place holder for no data.
     * @return The local mean.
     */
    private double getLocalMeanValue(final double[][] neighborPixelValues, final double noDataValue) {

        int k = 0;
        double mean = 0;
        for (int j = 0; j < filterSizeY; ++j) {
            for (int i = 0; i < filterSizeX; ++i) {
                if (neighborPixelValues[j][i] != noDataValue) {
                    mean += neighborPixelValues[j][i];
                    k++;
                }
            }
        }

        if (k > 0) {
            return mean/k;
        }

        return noDataValue;
    }

    /**
     * Compute local variance for pixels in the neighborhood.
     * @param mean The mean value for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue The place holder for no data.
     * @return The local variance.
     */
    private double getLocalVarianceValue(
            final double mean, final double[][] neighborPixelValues, final double noDataValue) {

        int k = 0;
        double var = 0.0;
        for (int j = 0; j < filterSizeY; ++j) {
            for (int i = 0; i < filterSizeX; ++i) {
                if (neighborPixelValues[j][i] != noDataValue) {
                    final double diff = neighborPixelValues[j][i] - mean;
                    var += diff * diff;
                    k++;
                }
            }
        }

        if (k > 1) {
            return var/(k-1);
        }

        return noDataValue;
    }

    /**
     * Compute local noise variance for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue The place holder for no data.
     * @return The local noise variance.
     */
    private static double getLocalNoiseVarianceValue(final double[][] neighborPixelValues, final double noDataValue) {

        final double[] subAreaVariances = new double[9];
        final double[] subArea = new double[9];
        int numSubArea = 0;
        for (int j = 0; j < 3; j++) {
            final int y0 = 2*j;
            for (int i = 0; i < 3; i++) {
                final int x0 = 2*i;

                int k = 0;
                for (int y = y0; y < y0 + 3; y++) {
                    final int yy = (y-y0)*3;
                    for (int x = x0; x < x0 + 3; x++) {
                        if (neighborPixelValues[y][x] != noDataValue) {
                            subArea[yy + (x-x0)] = neighborPixelValues[y][x];
                            k++;
                        }
                    }
                }

                if (k == 9) {
                    final double subAreaMean = getMeanValue(subArea, k);
                    if (subAreaMean > 0) {
                        subAreaVariances[numSubArea] = getVarianceValue(subArea, k, subAreaMean) / (subAreaMean*subAreaMean);
                    } else {
                        subAreaVariances[numSubArea] = 0.0;
                    }
                    numSubArea++;
                }
            }
        }

        Arrays.sort(subAreaVariances, 0, numSubArea-1);
        final int numSubAreaForAvg = Math.min(5, numSubArea);
        double avg = 0.0;
        for (int n = 0; n < numSubAreaForAvg; n++) {
            avg += subAreaVariances[n];
        }
        return avg / numSubAreaForAvg;
    }

    /**
     * Compute mean values for the 9 3x3 sub-areas in the 7x7 neighborhood.
     * @param neighborPixelValues The pixel values in the 7x7 neighborhood.
     * @param subAreaMeans The 9 mean values.
     */
    private static void computeSubAreaMeans(
            final double[][] neighborPixelValues, double[][] subAreaMeans) {

        for (int j = 0; j < 3; j++) {
            final int y0 = 2*j;
            for (int i = 0; i < 3; i++) {
                final int x0 = 2*i;

                int k = 0;
                double mean = 0.0;
                for (int y = y0; y < y0 + 3; y++) {
                    for (int x = x0; x < x0 + 3; x++) {
                        mean += neighborPixelValues[y][x];
                        k++;
                    }
                }

                subAreaMeans[j][i] = mean / k;
            }
        }
    }

    /**
     * Get pixel values from the non-edge area indicated by the given direction.
     * @param neighborPixelValues The pixel values in the 7x7 neighborhood.
     * @param d The direction index.
     * @param pixels The array of pixels.
     */
    private static void getNonEdgeAreaPixelValues(final double[][] neighborPixelValues, final int d,
                                                  final double[] pixels) {
        switch (d) {
        case 0: {

            int k = 0;
            for (int y = 0; y < 7; y++) {
                for (int x = 3; x < 7; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 1: {

            int k = 0;
            for (int y = 0; y < 7; y++) {
                for (int x = y; x < 7; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 2: {

            int k = 0;
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 7; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 3: {

            int k = 0;
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 7 - y; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 4: {

            int k = 0;
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 4; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 5: {

            int k = 0;
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < y + 1; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 6: {

            int k = 0;
            for (int y = 3; y < 7; y++) {
                for (int x = 0; x < 7; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        } case 7: {

            int k = 0;
            for (int y = 0; y < 7; y++) {
                for (int x = 6 - y; x < 7; x++) {
                    pixels[k] = neighborPixelValues[y][x];
                    k++;
                }
            }
            break;
        }
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SpeckleFilterOp.class);
            setOperatorUI(SpeckleFilterOpUI.class);
        }
    }
}

