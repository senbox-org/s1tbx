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

    private double cu, cu2;

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
    private static final double NonValidPixelValue = -1.0;

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
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final Rectangle sourceTileRectangle = getSourceTileRectangle(x0, y0, w, h);
            Tile sourceRaster1;
            Tile sourceRaster2 = null;
            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            Band sourceBand1;
            if (srcBandNames.length == 1) {
                sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
                sourceRaster1 = getSourceTile(sourceBand1, sourceTileRectangle);
                if (sourceRaster1 == null) {
                    throw new OperatorException("Cannot get source tile");
                }
            } else {
                sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
                Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
                sourceRaster1 = getSourceTile(sourceBand1, sourceTileRectangle);
                sourceRaster2 = getSourceTile(sourceBand2, sourceTileRectangle);
                if (sourceRaster1 == null || sourceRaster2 == null) {
                    throw new OperatorException("Cannot get source tile");
                }
            }
            final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);

            final Rectangle srcTileRectangle = sourceRaster1.getRectangle();
            final int sx0 = srcTileRectangle.x;
            final int sy0 = srcTileRectangle.y;
            final int sw = srcTileRectangle.width;
            final int sh = srcTileRectangle.height;

            final double[] neighborValues = new double[filterSizeX*filterSizeY];
            final ProductData trgData = targetTile.getDataBuffer();

            final ProductData srcData1 = sourceRaster1.getDataBuffer();
            ProductData srcData2 = null;
            if(sourceRaster2 != null)
                srcData2 = sourceRaster2.getDataBuffer();

            final TileIndex trgIndex = new TileIndex(targetTile);
            final TileIndex srcIndex = new TileIndex(sourceRaster1);

            switch (filter) {
                case MEAN_SPECKLE_FILTER:

                    computeMean(srcData1, srcData2, trgData, bandUnit, neighborValues, srcIndex, trgIndex,
                            x0, y0, w, h, sx0, sy0, sw, sh);

                    break;
                case MEDIAN_SPECKLE_FILTER:

                    computeMedian(srcData1, srcData2, trgData, bandUnit, neighborValues, srcIndex, trgIndex,
                            x0, y0, w, h, sx0, sy0, sw, sh);

                    break;
                case FROST_SPECKLE_FILTER:

                    computeFrost(srcData1, srcData2, trgData, bandUnit, neighborValues, srcIndex, trgIndex,
                            x0, y0, w, h, sx0, sy0, sw, sh);

                    break;
                case GAMMA_MAP_SPECKLE_FILTER:

                    if (estimateENL) {
                        computeEquivalentNumberOfLooks(sourceRaster1, sourceRaster2, bandUnit, x0, y0, w, h);
                    }
                    cu = 1.0 / Math.sqrt(enl);
                    cu2 = cu * cu;

                    computeGammaMap(srcData1, srcData2, trgData, bandUnit, neighborValues, srcIndex, trgIndex,
                            x0, y0, w, h, sx0, sy0, sw, sh);

                    break;
                case LEE_SPECKLE_FILTER:

                    if (estimateENL) {
                        computeEquivalentNumberOfLooks(sourceRaster1, sourceRaster2, bandUnit, x0, y0, w, h);
                    }
                    cu = 1.0 / Math.sqrt(enl);
                    cu2 = cu * cu;

                    computeLee(srcData1, srcData2, trgData, bandUnit, neighborValues, srcIndex, trgIndex,
                            x0, y0, w, h, sx0, sy0, sw, sh);

                    break;
                case LEE_REFINED_FILTER:

                    computeRefinedLee(srcData1, srcData2, trgData, bandUnit, srcIndex, trgIndex,
                            x0, y0, w, h, sx0, sy0, sw, sh);
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
     * @param trgData target ProductData
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Hight for the target_Tile_Rectangle.
     * @param sx0 src rect x
     * @param sy0 src rect y
     * @param sw src rect w
     * @param sh src rect h
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeMean(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                             final Unit.UnitType unit, final double[] neighborValues,
                             final TileIndex srcIndex, final TileIndex trgIndex,
                             final int x0, final int y0, final int w, final int h,
                             final int sx0, final int sy0, final int sw, final int sh) {

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            final int offset = trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                getNeighborValues(x, y, sx0, sy0, sw, sh, srcData1, srcData2, srcIndex, unit, neighborValues);

                trgData.setElemDoubleAt(x-offset, getMeanValue(neighborValues));
            }
        }
    }

    /**
     * Filter the given tile of image with Median filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Hight for the target_Tile_Rectangle.
     * @param sx0 src rect x
     * @param sy0 src rect y
     * @param sw src rect w
     * @param sh src rect h
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeMedian(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                               final Unit.UnitType unit, final double[] neighborValues,
                               final TileIndex srcIndex, final TileIndex trgIndex,
                               final int x0, final int y0, final int w, final int h,
                               final int sx0, final int sy0, final int sw, final int sh) {

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            final int offset = trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                getNeighborValues(x, y, sx0, sy0, sw, sh, srcData1, srcData2, srcIndex, unit, neighborValues);

                trgData.setElemDoubleAt(x-offset, getMedianValue(neighborValues));
            }
        }
    }

    /**
     * Filter the given tile of image with Frost filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Hight for the target_Tile_Rectangle.
     * @param sx0 src rect x
     * @param sy0 src rect y
     * @param sw src rect w
     * @param sh src rect h
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeFrost(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                              final Unit.UnitType unit, final double[] neighborValues,
                              final TileIndex srcIndex, final TileIndex trgIndex,
                              final int x0, final int y0, final int w, final int h,
                              final int sx0, final int sy0, final int sw, final int sh) {

        final double[] mask = new double[filterSizeX*filterSizeY];
        getFrostMask(mask);

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            final int offset = trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                getNeighborValues(x, y, sx0, sy0, sw, sh, srcData1, srcData2, srcIndex, unit, neighborValues);

                trgData.setElemDoubleAt(x-offset, getFrostValue(neighborValues, mask));
            }
        }
    }

    /**
     * Filter the given tile of image with Gamma filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Hight for the target_Tile_Rectangle.
     * @param sx0 src rect x
     * @param sy0 src rect y
     * @param sw src rect w
     * @param sh src rect h
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeGammaMap(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                                 final Unit.UnitType unit, final double[] neighborValues,
                                 final TileIndex srcIndex, final TileIndex trgIndex,
                                 final int x0, final int y0, final int w, final int h,
                                 final int sx0, final int sy0, final int sw, final int sh) {

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            final int offset = trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                getNeighborValues(x, y, sx0, sy0, sw, sh, srcData1, srcData2, srcIndex, unit, neighborValues);

                trgData.setElemDoubleAt(x-offset, getGammaMapValue(neighborValues));
            }
        }
    }

    /**
     * Filter the given tile of image with Lee filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param unit Unit for the 1st band.
     * @param neighborValues data to fill
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Hight for the target_Tile_Rectangle.
     * @param sx0 src rect x
     * @param sy0 src rect y
     * @param sw src rect w
     * @param sh src rect h
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    private void computeLee(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                            final Unit.UnitType unit, final double[] neighborValues,
                            final TileIndex srcIndex, final TileIndex trgIndex,
                            final int x0, final int y0, final int w, final int h,
                            final int sx0, final int sy0, final int sw, final int sh) {

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            final int offset = trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                getNeighborValues(x, y, sx0, sy0, sw, sh, srcData1, srcData2, srcIndex, unit, neighborValues);

                trgData.setElemDoubleAt(x-offset, getLeeValue(neighborValues));
            }
        }
    }

    /**
     * Get pixel values in a filter size rectanglar region centered at the given pixel.
     * @param x X coordinate of a given pixel.
     * @param y Y coordinate of a given pixel.
     * @param sx0 X coordinate of pixel at upper left corner of source tile.
     * @param sy0 Y coordinate of pixel at upper left corner of source tile.
     * @param sw Source tile width.
     * @param sh Source tile height.
     * @param srcData1 The source ProductData for 1st band.
     * @param srcData2 The source ProductData for 2nd band.
     * @param bandUnit Unit for the 1st band.
     * @param neighborValues Array holding the pixel values.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in obtaining the pixel values.
     */
    private void getNeighborValues(final int x, final int y, final int sx0, final int sy0, final int sw, final int sh,
                                   final ProductData srcData1, final ProductData srcData2, final TileIndex srcIndex,
                                   final Unit.UnitType bandUnit, final double[] neighborValues) {

        if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {

            for (int j = 0; j < filterSizeY; ++j) {

                int yj = y - halfSizeY + j;
                if (yj < sy0) {
                    yj = sy0;
                } else if (yj >= sy0 + sh) {
                    yj = sy0 + sh - 1;
                }

                final int stride = j*filterSizeX;
                final int offset = srcIndex.calculateStride(yj);
                for (int i = 0; i < filterSizeX; ++i) {

                    int xi = x - halfSizeX + i;
                    if (xi < sx0) {
                        xi = sx0;
                    } else if (xi >= sx0 + sw) {
                        xi = sx0 + sw - 1;
                    }
                    int idx = xi-offset;
                    double I = srcData1.getElemDoubleAt(idx);
                    double Q = srcData2.getElemDoubleAt(idx);
                    neighborValues[stride + j] = I*I + Q*Q;
                }
            }

        } else {

            for (int j = 0; j < filterSizeY; ++j) {

                int yj = y - halfSizeY + j;
                if (yj < sy0) {
                    yj = sy0;
                } else if (yj >= sy0 + sh) {
                    yj = sy0 + sh - 1;
                }
                final int stride = j*filterSizeX;
                final int offset = srcIndex.calculateStride(yj);
                for (int i = 0; i < filterSizeX; ++i) {

                    int xi = x - halfSizeX + i;
                    if (xi < sx0) {
                        xi = sx0;
                    } else if (xi >= sx0 + sw) {
                        xi = sx0 + sw - 1;
                    }
                    neighborValues[stride + i] = srcData1.getElemDoubleAt(xi-offset);
                }
            }
        }
    }

    /**
     * Get the mean value of pixel intensities in a given rectanglar region.
     * @param neighborValues The pixel values in the given rectanglar region.
     * @return mean The mean value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the mean value.
     */
    private static double getMeanValue(final double[] neighborValues) {

        double mean = 0.0;
        for (double neighborValue : neighborValues) {
            mean += neighborValue;
        }
        mean /= neighborValues.length;

        return mean;
    }

    /**
     * Get the variance of pixel intensities in a given rectanglar region.
     * @param neighborValues The pixel values in the given rectanglar region.
     * @param mean the mean of neighborValues
     * @return var The variance value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the variance.
     */
    private static double getVarianceValue(final double[] neighborValues, final double mean) {

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

    /**
     * Get the median value of pixel intensities in a given rectanglar region.
     * @param neighborValues Array holding pixel values.
     * @return median The median value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the median value.
     */
    private static double getMedianValue(final double[] neighborValues) {

        Arrays.sort(neighborValues);

        // Then get the median value
        return neighborValues[(neighborValues.length / 2)];
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
     * Get the Frost filtered pixel intensity for pixels in a given rectanglar region.
     * @param neighborValues Array holding the pixel valuses.
     * @param mask Array holding Frost filter mask values.
     * @return val The Frost filtered value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the Frost filtered value.
     */
    private double getFrostValue(final double[] neighborValues, final double[] mask) {

        final double mean = getMeanValue(neighborValues);
        if (mean <= Double.MIN_VALUE) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, mean);
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
     * Get the Gamma filtered pixel intensity for pixels in a given rectanglar region.
     * @param neighborValues Array holding the pixel valuses.
     * @return val The Gamma filtered value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the Gamma filtered value.
     */
    private double getGammaMapValue(final double[] neighborValues) {

        final double mean = getMeanValue(neighborValues);
        if (mean <= Double.MIN_VALUE) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, mean);
        if (var <= Double.MIN_VALUE) {
            return mean;
        }

        final double ci = Math.sqrt(var) / mean;
        if (ci <= cu) {
            return mean;
        }

        final double cp = neighborValues[(neighborValues.length/2)];

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
     * Get the Lee filtered pixel intensity for pixels in a given rectanglar region.
     * @param neighborValues Array holding the pixel valuses.
     * @return val The Lee filtered value.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in computation of the Lee filtered value.
     */
    private double getLeeValue(final double[] neighborValues) {

        final double mean = getMeanValue(neighborValues);
        if (Double.compare(mean, Double.MIN_VALUE) <= 0) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, mean);
        if (Double.compare(var, Double.MIN_VALUE) <= 0) {
            return mean;
        }

        final double ci = Math.sqrt(var) / mean;
        if (ci < cu) {
            return mean;
        }

        final double cp = neighborValues[(neighborValues.length/2)];
        final double w = 1 - cu2 / (ci*ci);

        return cp*w + mean*(1 - w);
    }

    /**
     * Compute the equivalent number of looks.
     * @param sourceRaster1 The source tile for 1st band.
     * @param sourceRaster2 The source tile for 2nd band.
     * @param bandUnit Unit for 1st band.
     * @param x0 X coordinate of the upper left corner point of the target tile rectangle.
     * @param y0 Y coordinate of the upper left corner point of the target tile rectangle.
     * @param w The width of the target tile rectangle.
     * @param h The height of the target tile rectangle.
     */
    void computeEquivalentNumberOfLooks(final Tile sourceRaster1, final Tile sourceRaster2, final Unit.UnitType bandUnit,
                                        final int x0, final int y0, final int w, final int h) {

        final ProductData srcData1 = sourceRaster1.getDataBuffer();
        ProductData srcData2 = null;
        if(sourceRaster2 != null)
            srcData2 = sourceRaster2.getDataBuffer();

        if (bandUnit != null && (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY)) {
            double sum = 0;
            double sum2 = 0;
            for (int y = y0; y < y0 + h; y++) {
                for (int x = x0; x < x0 + w; x++) {

                    final int idx = sourceRaster1.getDataBufferIndex(x, y);
                    final double i = srcData1.getElemDoubleAt(idx);
                    final double q = srcData2.getElemDoubleAt(idx);
                    double v = i*i + q*q;
                    sum += v;
                    sum2 += v*v;
                }
            }

            final double area = h * w;
            final double m = sum / area;
            final double m2 = sum2 / area;
            final double mm = m*m;
            enl = mm / (m2 - mm);

        } else if (bandUnit != null && bandUnit == Unit.UnitType.INTENSITY) {
            double sum = 0;
            double sum2 = 0;
            for (int y = y0; y < y0 + h; y++) {
                for (int x = x0; x < x0 + w; x++) {

                    final double v = srcData1.getElemDoubleAt(sourceRaster1.getDataBufferIndex(x, y));
                    sum += v;
                    sum2 += v*v;
                }
            }

            final double area = h * w;
            final double m = sum / area;
            final double m2 = sum2 / area;
            final double mm = m*m;
            enl = mm / (m2 - mm);

        } else {

            double sum2 = 0;
            double sum4 = 0;
            for (int y = y0; y < y0 + h; y++) {
                for (int x = x0; x < x0 + w; x++) {

                    final double v = srcData1.getElemDoubleAt(sourceRaster1.getDataBufferIndex(x, y));
                    final double v2 = v*v;
                    sum2 += v2;
                    sum4 += v2*v2;
                }
            }

            final double area = h * w;
            final double m2 = sum2 / area;
            final double m4 = sum4 / area;
            final double m2m2 = m2*m2;
            enl = m2m2 / (m4 - m2m2);
        }
    }

    /**
     * Filter the given tile of image with refined Lee filter.
     * @param srcData1 The source ProductData for the 1st band.
     * @param srcData2 The source ProductData for the 2nd band.
     * @param trgData target ProductData
     * @param unit Unit for the 1st band.
     * @param x0 X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Hight for the target_Tile_Rectangle.
     * @param sx0 src rect x
     * @param sy0 src rect y
     * @param sw src rect w
     * @param sh src rect h
     */
    private void computeRefinedLee(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                                   final Unit.UnitType unit,
                                   final TileIndex srcIndex, final TileIndex trgIndex,
                                   final int x0, final int y0, final int w, final int h,
                                   final int sx0, final int sy0, final int sw, final int sh) {

        final double[][] neighborPixelValues = new double[filterSizeY][filterSizeX];
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            final int offset = trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int n = getNeighborValuesWithoutBorderExt(
                        x, y, sx0, sy0, sw, sh, srcData1, srcData2, unit, neighborPixelValues, srcIndex);

                trgData.setElemDoubleAt(x-offset, getRefinedLeeValue(n, neighborPixelValues));
            }
        }
    }

    /**
     * Get pixel intensities in a filter size rectanglar region centered at the given pixel.
     * @param x X coordinate of the given pixel.
     * @param y Y coordinate of the given pixel.
     * @param sx0 X coordinate of pixel at upper left corner of source tile.
     * @param sy0 Y coordinate of pixel at upper left corner of source tile.
     * @param sw Source tile width.
     * @param sh Source tile height.
     * @param srcData1 databuffer 1
     * @param srcData2 databuffer 2
     * @param bandUnit Unit for the 1st band.
     * @param neighborPixelValues 2-D array holding the pixel valuse.
     * @return The number of valid pixels.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs in obtaining the pixel values.
     */
    private int getNeighborValuesWithoutBorderExt(final int x, final int y, final int sx0, final int sy0,
                                                  final int sw, final int sh,
                                                  final ProductData srcData1, final ProductData srcData2,
                                                  final Unit.UnitType bandUnit,
                                                  final double[][] neighborPixelValues,
                                                  final TileIndex srcIndex) {
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;
        int k = 0;
        if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {

            for (int j = 0; j < filterSizeY; ++j) {
                final int yj = y - halfSizeY + j;
                if(yj < sy0 || yj >= maxY) {
                    for (int i = 0; i < filterSizeX; ++i) {
                        neighborPixelValues[j][i] = NonValidPixelValue;
                    }
                    continue;
                }
                final int offset = srcIndex.calculateStride(yj);
                for (int i = 0; i < filterSizeX; ++i) {
                    final int xi = x - halfSizeX + i;
                    if (xi < sx0 || xi >= maxX) {
                        neighborPixelValues[j][i] = NonValidPixelValue;
                    } else {
                        final int idx = xi-offset;
                        final double I = srcData1.getElemDoubleAt(idx);
                        final double Q = srcData2.getElemDoubleAt(idx);
                        neighborPixelValues[j][i] = I*I + Q*Q;
                        k++;
                    }
                }
            }

        } else {

            for (int j = 0; j < filterSizeY; ++j) {
                final int yj = y - halfSizeY + j;
                if(yj < sy0 || yj >= maxY) {
                    for (int i = 0; i < filterSizeX; ++i) {
                        neighborPixelValues[j][i] = NonValidPixelValue;
                    }
                    continue;
                }
                final int offset = srcIndex.calculateStride(yj);
                for (int i = 0; i < filterSizeX; ++i) {
                    final int xi = x - halfSizeX + i;
                    if (xi < sx0 || xi >= maxX) {
                        neighborPixelValues[j][i] = NonValidPixelValue;
                    } else {
                        neighborPixelValues[j][i] = srcData1.getElemDoubleAt(xi-offset);
                        k++;
                    }
                }
            }
        }

        return k;
    }

    /**
     * Compute filtered pixel value using refined Lee filter.
     * @param n The number of valid pixel in the neighborhood.
     * @param neighborPixelValues The neighbor pixel values.
     * @return The filtered pixel value.
     */
    private double getRefinedLeeValue(final int n, final double[][] neighborPixelValues) {

        if (n < filterSizeX*filterSizeY) {
            return computePixelValueUsingLocalStatistics(neighborPixelValues);
        }

        final double var = getLocalVarianceValue(getLocalMeanValue(neighborPixelValues), neighborPixelValues);
        if (var < edgeThreshold) {
            return computePixelValueUsingLocalStatistics(neighborPixelValues);
        }

        return computePixelValueUsingEdgeDetection(neighborPixelValues);
    }

    /**
     * Compute filtered pixel value using Local Statistics filter.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The filtered pixel value.
     */
    private double computePixelValueUsingLocalStatistics(final double[][] neighborPixelValues) {
        // y is the pixel amplitude or intensity and x is the pixel reflectance before degradation
        final double meanY = getLocalMeanValue(neighborPixelValues);
        final double varY = getLocalVarianceValue(meanY, neighborPixelValues);
        if (varY == 0.0) {
            return 0.0;
        }
        final double sigmaV = getLocalNoiseVarianceValue(neighborPixelValues);
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
     * @return The filtered pixel value.
     */
    private static double computePixelValueUsingEdgeDetection(final double[][] neighborPixelValues) {

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

        final double meanY = getMeanValue(pixels);
        final double varY = getVarianceValue(pixels, meanY);
        if (varY == 0.0) {
            return 0.0;
        }
        final double sigmaV = getLocalNoiseVarianceValue(neighborPixelValues);
        double varX = (varY - meanY*meanY*sigmaV) / (1 + sigmaV);
        if (varX < 0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b*(neighborPixelValues[3][3] - meanY);
    }

    /**
     * Comppute local mean for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The local mean.
     */
    private double getLocalMeanValue(final double[][] neighborPixelValues) {
        int k = 0;
        double mean = 0;
        for (int j = 0; j < filterSizeY; ++j) {
            for (int i = 0; i < filterSizeX; ++i) {
                if (neighborPixelValues[j][i] != NonValidPixelValue) {
                    mean += neighborPixelValues[j][i];
                    k++;
                }
            }
        }
        return mean/k;
    }

    /**
     * Comppute local variance for pixels in the neighborhood.
     * @param mean The mean value for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The local variance.
     */
    private double getLocalVarianceValue(final double mean, final double[][] neighborPixelValues) {
        int k = 0;
        double var = 0.0;
        for (int j = 0; j < filterSizeY; ++j) {
            for (int i = 0; i < filterSizeX; ++i) {
                if (neighborPixelValues[j][i] != NonValidPixelValue) {
                    final double diff = neighborPixelValues[j][i] - mean;
                    var += diff * diff;
                    k++;
                }
            }
        }
        return var/(k-1);
    }

    /**
     * Comppute local noise variance for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The local noise variance.
     */
    private static double getLocalNoiseVarianceValue(final double[][] neighborPixelValues) {

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
                        if (neighborPixelValues[y][x] != NonValidPixelValue) {
                            subArea[yy + (x-x0)] = neighborPixelValues[y][x];
                            k++;
                        }
                    }
                }

                if (k == 9) {
                    final double subAreaMean = getMeanValue(subArea);
                    if (subAreaMean > 0) {
                        subAreaVariances[numSubArea] = getVarianceValue(subArea, subAreaMean) / (subAreaMean*subAreaMean);
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
    private static void computeSubAreaMeans(final double[][] neighborPixelValues, double[][] subAreaMeans) {
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

