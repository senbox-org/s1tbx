/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.filtering;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.primitives.Doubles;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.*;

/**
 * Applies a Speckle Filter to the data
 */
@OperatorMetadata(alias = "Speckle-Filter",
        category = "Radar/Speckle Filtering",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Speckle Reduction")
public class SpeckleFilterOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {NONE, MEAN_SPECKLE_FILTER, MEDIAN_SPECKLE_FILTER, FROST_SPECKLE_FILTER,
            GAMMA_MAP_SPECKLE_FILTER, LEE_SPECKLE_FILTER, LEE_REFINED_FILTER, LEE_SIGMA_FILTER},
            defaultValue = LEE_REFINED_FILTER,
            label = "Filter")
    private String filter;

    @Parameter(description = "The kernel x dimension", interval = "(1, 100]", defaultValue = "3", label = "Size X")
    private int filterSizeX = 3;

    @Parameter(description = "The kernel y dimension", interval = "(1, 100]", defaultValue = "3", label = "Size Y")
    private int filterSizeY = 3;

    @Parameter(description = "The damping factor (Frost filter only)", interval = "(0, 100]", defaultValue = "2",
            label = "Frost Damping Factor")
    private int dampingFactor = 2;

    @Parameter(description = "The edge threshold (Refined Lee filter only)", interval = "(0, *)", defaultValue = "5000",
            label = "Edge detection threshold")
    private double edgeThreshold = 5000.0;

    @Parameter(defaultValue = "false", label = "Estimate Eqivalent Number of Looks")
    private boolean estimateENL = true;

    @Parameter(description = "The number of looks", interval = "(0, *)", defaultValue = "1.0",
            label = "Number of looks")
    private double enl = 1.0;

    @Parameter(valueSet = {NUM_LOOKS_1, NUM_LOOKS_2, NUM_LOOKS_3, NUM_LOOKS_4},
            defaultValue = NUM_LOOKS_1, label = "Number of Looks")
    private String numLooksStr = NUM_LOOKS_1;

    @Parameter(valueSet = {WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11},
            defaultValue = WINDOW_SIZE_7x7, label = "Window Size")
    private String windowSize = WINDOW_SIZE_7x7; // window size for all filters

    @Parameter(valueSet = {WINDOW_SIZE_3x3, WINDOW_SIZE_5x5}, defaultValue = WINDOW_SIZE_3x3,
            label = "Point target window Size")
    private String targetWindowSizeStr = WINDOW_SIZE_3x3; // window size for point target determination in Lee sigma

    @Parameter(valueSet = {SIGMA_50_PERCENT, SIGMA_60_PERCENT, SIGMA_70_PERCENT, SIGMA_80_PERCENT, SIGMA_90_PERCENT},
            defaultValue = SIGMA_90_PERCENT, label = "Point target window Size")
    private String sigmaStr = SIGMA_90_PERCENT; // sigma value in Lee sigma

    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<>();
    private int halfSizeX;
    private int halfSizeY;
    private int sourceImageWidth;
    private int sourceImageHeight;

    public static final String NONE = "None";
    public static final String MEAN_SPECKLE_FILTER = "Mean";
    public static final String MEDIAN_SPECKLE_FILTER = "Median";
    public static final String FROST_SPECKLE_FILTER = "Frost";
    public static final String GAMMA_MAP_SPECKLE_FILTER = "Gamma Map";
    public static final String LEE_SPECKLE_FILTER = "Lee";
    public static final String LEE_REFINED_FILTER = "Refined Lee";
    public static final String LEE_SIGMA_FILTER = "Improved Lee Sigma Filter";

    // parameters for LEE_REFINED_FILTER
    private int numLooks = 0;
    private int filterSize = 0;
    private double A1, A2; // sigma range for amplitude
    private double I1, I2; // sigma range for intensity
    private int sigma;
    private double ISigmaV;
    private double ISigmaVSqr;
    private double ISigmaVP; // revised sigmaV used in MMSE filter
    private double ISigmaVPSqr;
    private double ASigmaV;
    private double ASigmaVSqr;
    private double ASigmaVP; // revised sigmaV used in MMSE filter
    private double ASigmaVPSqr;
    private int targetWindowSize = 0;
    private int halfTargetWindowSize = 0;
    private int targetSize = 5;

    public static final String WINDOW_SIZE_3x3 = "3x3";
    public static final String WINDOW_SIZE_5x5 = "5x5";
    public static final String WINDOW_SIZE_7x7 = "7x7";
    public static final String WINDOW_SIZE_9x9 = "9x9";
    public static final String WINDOW_SIZE_11x11 = "11x11";

    public static final String NUM_LOOKS_1 = "1";
    public static final String NUM_LOOKS_2 = "2";
    public static final String NUM_LOOKS_3 = "3";
    public static final String NUM_LOOKS_4 = "4";

    public static final String SIGMA_50_PERCENT = "0.5";
    public static final String SIGMA_60_PERCENT = "0.6";
    public static final String SIGMA_70_PERCENT = "0.7";
    public static final String SIGMA_80_PERCENT = "0.8";
    public static final String SIGMA_90_PERCENT = "0.9";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SpeckleFilterOp() {
    }

    /**
     * Set speckle filter. This function is used by unit test only.
     *
     * @param s The filter name.
     */
    public void SetFilter(String s) {

        if (s.equals(MEAN_SPECKLE_FILTER) ||
                s.equals(MEDIAN_SPECKLE_FILTER) ||
                s.equals(FROST_SPECKLE_FILTER) ||
                s.equals(GAMMA_MAP_SPECKLE_FILTER) ||
                s.equals(LEE_SPECKLE_FILTER) ||
                s.equals(LEE_REFINED_FILTER) ||
                s.equals(LEE_SIGMA_FILTER)) {
            filter = s;
        } else {
            throw new OperatorException(s + " is an invalid filter name.");
        }
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfTOPSARBurstProduct(false);

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            if (filter.equals(LEE_SIGMA_FILTER)) {
                setLeeSigmaParameters();
            } else {

                if (filter.equals(LEE_REFINED_FILTER)) {
                    filterSizeX = 7;
                    filterSizeY = 7;
                }

                if (filterSizeX % 2 == 0 || filterSizeY % 2 == 0) {
                    throw new OperatorException("Please choose an odd number for filter size");
                }

                halfSizeX = filterSizeX / 2;
                halfSizeY = filterSizeY / 2;
            }

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     *
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        if(filter.equals(NONE)) {
            final Band[] selectedBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
            for (Band srcBand : selectedBands) {
                if (srcBand instanceof VirtualBand) {
                    final VirtualBand sourceBand = (VirtualBand) srcBand;
                    final VirtualBand targetBand = new VirtualBand(sourceBand.getName(),
                            sourceBand.getDataType(),
                            sourceBand.getRasterWidth(),
                            sourceBand.getRasterHeight(),
                            sourceBand.getExpression());
                    ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                    targetProduct.addBand(targetBand);
                } else {
                    ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                }
            }
        } else {
            OperatorUtils.addSelectedBands(
                    sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, true, true);
        }

        updateTargetProductMetadata();
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if(absTgt != null) {
            absTgt.setAttributeString(AbstractMetadata.SAMPLE_TYPE, "DETECTED");
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
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

            final double[] neighborValues = new double[filterSizeX * filterSizeY];

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
                case LEE_SIGMA_FILTER:

                    computeLeeSigma(sourceData1, sourceData2, trgData, noDataValue, bandUnit,
                            srcIndex, trgIndex, tx0, ty0, tw, th, sourceTileRectangle, sourceTile1);
                    break;
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param x0 X coordinate of the upper left corner point of the target tile rectangle.
     * @param y0 Y coordinate of the upper left corner point of the target tile rectangle.
     * @param w  The width of the target tile rectangle.
     * @param h  The height of the target tile rectangle.
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
     *
     * @param srcData1       The source ProductData for the 1st band.
     * @param srcData2       The source ProductData for the 2nd band.
     * @param trgData        target ProductData.
     * @param noDataValue    the place holder for no data.
     * @param unit           Unit for the 1st band.
     * @param neighborValues data to fill.
     * @param srcIndex       The source tile index.
     * @param trgIndex       The target tile index.
     * @param tx0            X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param ty0            Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param tw             Width for the target_Tile_Rectangle.
     * @param th             Height for the target_Tile_Rectangle.
     * @throws OperatorException If an error occurs during computation of the filtered value.
     */
    private void computeMean(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                             final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                             final TileIndex srcIndex, final TileIndex trgIndex,
                             final int tx0, final int ty0, final int tw, final int th) {

        final boolean isComplex = unit == Unit.UnitType.REAL || unit == Unit.UnitType.IMAGINARY;
        final int maxY = ty0 + th;
        final int maxX = tx0 + tw;
        for (int y = ty0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = tx0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, isComplex, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getMeanValue(neighborValues, numSamples, noDataValue));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Filter the given tile of image with Median filter.
     *
     * @param srcData1       The source ProductData for the 1st band.
     * @param srcData2       The source ProductData for the 2nd band.
     * @param trgData        target ProductData
     * @param noDataValue    the place holder for no data
     * @param unit           Unit for the 1st band.
     * @param neighborValues data to fill
     * @param srcIndex       The source tile index.
     * @param trgIndex       The target tile index.
     * @param x0             X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0             Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w              Width for the target_Tile_Rectangle.
     * @param h              Height for the target_Tile_Rectangle.
     * @throws OperatorException If an error occurs during computation of the filtered value.
     */
    private void computeMedian(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                               final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                               final TileIndex srcIndex, final TileIndex trgIndex,
                               final int x0, final int y0, final int w, final int h) {

        final boolean isComplex = unit == Unit.UnitType.REAL || unit == Unit.UnitType.IMAGINARY;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, isComplex, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getMedianValue(neighborValues, numSamples, noDataValue));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Filter the given tile of image with Frost filter.
     *
     * @param srcData1       The source ProductData for the 1st band.
     * @param srcData2       The source ProductData for the 2nd band.
     * @param trgData        target ProductData.
     * @param noDataValue    the place holder for no data.
     * @param unit           Unit for the 1st band.
     * @param neighborValues data to fill.
     * @param srcIndex       The source tile index.
     * @param trgIndex       The target tile index.
     * @param x0             X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0             Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w              Width for the target_Tile_Rectangle.
     * @param h              Height for the target_Tile_Rectangle.
     * @throws OperatorException If an error occurs during computation of the filtered value.
     */
    private void computeFrost(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                              final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                              final TileIndex srcIndex, final TileIndex trgIndex,
                              final int x0, final int y0, final int w, final int h) {

        final boolean isComplex = unit == Unit.UnitType.REAL || unit == Unit.UnitType.IMAGINARY;
        final double[] mask = new double[filterSizeX * filterSizeY];
        getFrostMask(mask);

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, isComplex, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getFrostValue(neighborValues, numSamples, noDataValue, mask));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Filter the given tile of image with Gamma filter.
     *
     * @param srcData1       The source ProductData for the 1st band.
     * @param srcData2       The source ProductData for the 2nd band.
     * @param trgData        target ProductData
     * @param noDataValue    the place holder for no data.
     * @param unit           Unit for the 1st band.
     * @param neighborValues data to fill.
     * @param srcIndex       The source tile index.
     * @param trgIndex       The target tile index.
     * @param x0             X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0             Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w              Width for the target_Tile_Rectangle.
     * @param h              Height for the target_Tile_Rectangle.
     * @throws OperatorException If an error occurs during computation of the filtered value.
     */
    private void computeGammaMap(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                                 final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                                 final TileIndex srcIndex, final TileIndex trgIndex,
                                 final int x0, final int y0, final int w, final int h,
                                 final double cu, final double cu2, final double enl) {

        final boolean isComplex = unit == Unit.UnitType.REAL || unit == Unit.UnitType.IMAGINARY;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, isComplex, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getGammaMapValue(neighborValues, numSamples, noDataValue, cu, cu2, enl));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Filter the given tile of image with Lee filter.
     *
     * @param srcData1       The source ProductData for the 1st band.
     * @param srcData2       The source ProductData for the 2nd band.
     * @param trgData        target ProductData
     * @param noDataValue    the place holder for no data.
     * @param unit           Unit for the 1st band.
     * @param neighborValues data to fill.
     * @param srcIndex       The source tile index.
     * @param trgIndex       The target tile index.
     * @param x0             X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0             Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w              Width for the target_Tile_Rectangle.
     * @param h              Height for the target_Tile_Rectangle.
     * @throws OperatorException If an error occurs during computation of the filtered value.
     */
    private void computeLee(final ProductData srcData1, final ProductData srcData2, final ProductData trgData,
                            final double noDataValue, final Unit.UnitType unit, final double[] neighborValues,
                            final TileIndex srcIndex, final TileIndex trgIndex,
                            final int x0, final int y0, final int w, final int h,
                            final double cu, final double cu2) {

        final boolean isComplex = unit == Unit.UnitType.REAL || unit == Unit.UnitType.IMAGINARY;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                final int numSamples = getNeighborValues(
                        x, y, srcData1, srcData2, srcIndex, noDataValue, isComplex, neighborValues);

                if (numSamples > 0) {
                    trgData.setElemDoubleAt(idx, getLeeValue(neighborValues, numSamples, noDataValue, cu, cu2));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
    }

    /**
     * Get pixel values in a filter size rectangular region centered at the given pixel.
     *
     * @param tx             X coordinate of a given pixel.
     * @param ty             Y coordinate of a given pixel.
     * @param srcData1       The source ProductData for 1st band.
     * @param srcData2       The source ProductData for 2nd band.
     * @param srcIndex       The source tile index.
     * @param noDataValue    Place holder for no data value.
     * @param isComplex      has i and q or not
     * @param neighborValues Array holding the pixel values.
     * @return The number of valid samples.
     * @throws OperatorException If an error occurs in obtaining the pixel values.
     */
    private int getNeighborValues(final int tx, final int ty, final ProductData srcData1, final ProductData srcData2,
                                  final TileIndex srcIndex, final double noDataValue, final boolean isComplex,
                                  final double[] neighborValues) {
        final int minX = tx - halfSizeX;
        final int maxX = tx + halfSizeX;
        final int minY = ty - halfSizeY;
        final int maxY = ty + halfSizeY;
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
                            if (I != noDataValue && Q != noDataValue) {
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
                            if (v != noDataValue) {
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
    private static double getMeanValue(final double[] neighborValues, final int numSamples, final double noDataValue) {

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
    private static double getVarianceValue(
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
     * Get the median value of pixel intensities in a given rectangular region.
     *
     * @param neighborValues Array holding pixel values.
     * @param numSamples     The number of samples.
     * @param noDataValue    Place holder for no data value.
     * @return median The median value.
     * @throws OperatorException If an error occurs in computation of the median value.
     */
    private static double getMedianValue(
            final double[] neighborValues, final int numSamples, final double noDataValue) {

        double[] tmp = new double[numSamples];
        int k = 0;
        for (double v : neighborValues) {
            if (v != noDataValue) {
                tmp[k++] = v;
            }
        }
        Arrays.sort(tmp);
        return tmp[numSamples / 2];
    }

    /**
     * Get Frost mask for given Frost filter size.
     *
     * @param mask Array holding Frost filter mask values.
     * @throws OperatorException If an error occurs in computation of the Frost mask.
     */
    private void getFrostMask(final double[] mask) {

        for (int i = 0; i < filterSizeX; i++) {

            final int s = i * filterSizeY;
            final int dr = Math.abs(i - halfSizeX);

            for (int j = 0; j < filterSizeY; j++) {
                mask[j + s] = Math.max(dr, Math.abs(j - halfSizeY));
            }
        }
    }

    /**
     * Get the Frost filtered pixel intensity for pixels in a given rectangular region.
     *
     * @param neighborValues Array holding the pixel values.
     * @param numSamples     The number of samples.
     * @param noDataValue    Place holder for no data value.
     * @param mask           Array holding Frost filter mask values.
     * @return val The Frost filtered value.
     * @throws OperatorException If an error occurs in computation of the Frost filtered value.
     */
    private double getFrostValue(
            final double[] neighborValues, final int numSamples, final double noDataValue, final double[] mask) {

        final double mean = getMeanValue(neighborValues, numSamples, noDataValue);
        if (mean <= Double.MIN_VALUE) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, numSamples, mean, noDataValue);
        if (var <= Double.MIN_VALUE) {
            return mean;
        }

        final double k = dampingFactor * var / (mean * mean);

        double sum = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < neighborValues.length; i++) {
            if (neighborValues[i] != noDataValue) {
                final double weight = FastMath.exp(-k * mask[i]);
                sum += weight * neighborValues[i];
                totalWeight += weight;
            }
        }
        return sum / totalWeight;
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

    /**
     * Get the Lee filtered pixel intensity for pixels in a given rectangular region.
     *
     * @param neighborValues Array holding the pixel values.
     * @param numSamples     The number of samples.
     * @param noDataValue    Place holder for no data value.
     * @return val The Lee filtered value.
     * @throws OperatorException If an error occurs in computation of the Lee filtered value.
     */
    private double getLeeValue(final double[] neighborValues, final int numSamples, final double noDataValue,
                               final double cu, final double cu2) {

        final double mean = getMeanValue(neighborValues, numSamples, noDataValue);
        if (Double.compare(mean, Double.MIN_VALUE) <= 0) {
            return mean;
        }

        final double var = getVarianceValue(neighborValues, numSamples, mean, noDataValue);
        if (Double.compare(var, Double.MIN_VALUE) <= 0) {
            return mean;
        }

        final double ci = Math.sqrt(var) / mean;
        if (ci < cu) {
            return mean;
        }

        final double cp = neighborValues[neighborValues.length / 2];
        final double w = 1 - cu2 / (ci * ci);

        return cp * w + mean * (1 - w);
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
     * Filter the given tile of image with refined Lee filter.
     *
     * @param srcData1    The source ProductData for the 1st band.
     * @param srcData2    The source ProductData for the 2nd band.
     * @param trgData     target ProductData
     * @param noDataValue The place holder for no data.
     * @param unit        Unit for the 1st band.
     * @param srcIndex    The source tile index.
     * @param trgIndex    The target tile index.
     * @param x0          X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0          Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w           Width for the target_Tile_Rectangle.
     * @param h           Height for the target_Tile_Rectangle.
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
                    trgData.setElemDoubleAt(idx, getRefinedLeeValueUsingEdgeThreshold(
                            filterSizeX, filterSizeY, edgeThreshold, numSamples, noDataValue, neighborPixelValues));
                } else {
                    trgData.setElemDoubleAt(idx, noDataValue);
                }
            }
        }
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
     * @param neighborPixelValues 2-D array holding the pixel values.
     * @return The number of valid pixels.
     * @throws OperatorException If an error occurs in obtaining the pixel values.
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

    /**
     * Compute filtered pixel value using refined Lee filter.
     *
     * @param numSamples          The number of valid pixel in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @param neighborPixelValues The neighbor pixel values.
     * @return The filtered pixel value.
     */
    public static double getRefinedLeeValueUsingEdgeThreshold(
            final int filterSizeX, final int filterSizeY, final double edgeThreshold,
            final int numSamples, final double noDataValue, final double[][] neighborPixelValues) {

        if (numSamples < filterSizeX * filterSizeY) {
            return computePixelValueUsingLocalStatistics(neighborPixelValues, noDataValue);
        }

        final double var = getLocalVarianceValue(
                getLocalMeanValue(neighborPixelValues, noDataValue), neighborPixelValues, noDataValue);
        if (var < edgeThreshold) {
            return computePixelValueUsingLocalStatistics(neighborPixelValues, noDataValue);
        }

        final double[][] subAreaMeans = new double[3][3];
        computeSubAreaMeans(neighborPixelValues, subAreaMeans);

        final double[] gradients = new double[4];
        computeGradients(subAreaMeans, gradients);

        return computePixelValueUsingEdgeDetection(neighborPixelValues, noDataValue, subAreaMeans, gradients);
    }

    public static double getRefinedLeeValueUsingGradientThreshold(
            final int filterSizeX, final int filterSizeY, final double gradThreshold,
            final int numSamples, final double noDataValue, final double[][] neighborPixelValues) {

        if (numSamples < filterSizeX * filterSizeY) {
            return getMeanValue(neighborPixelValues, noDataValue);
        }

        final double[][] subAreaMeans = new double[3][3];
        computeSubAreaMeans(neighborPixelValues, subAreaMeans);

        final double[] gradients = new double[4];
        computeGradients(subAreaMeans, gradients);

        if (gradients[0] < gradThreshold && gradients[1] < gradThreshold &&
                gradients[2] < gradThreshold && gradients[3] < gradThreshold) {
            return getMeanValue(neighborPixelValues, noDataValue);
        }

        return computePixelValueUsingEdgeDetection(neighborPixelValues, noDataValue, subAreaMeans, gradients);
    }

    private static double getMeanValue(final double[][] neighborValues, final double noDataValue) {

        double mean = 0.0;
        int numSamples = 0;
        for (double[] row : neighborValues) {
            for (double v : row) {
                if (v != noDataValue) {
                    mean += v;
                    numSamples++;
                }
            }
        }
        mean /= numSamples;

        return mean;
    }

    /**
     * Compute filtered pixel value using Local Statistics filter.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The filtered pixel value.
     */
    private static double computePixelValueUsingLocalStatistics(
            final double[][] neighborPixelValues, final double noDataValue) {

        if (neighborPixelValues[neighborPixelValues.length/2][neighborPixelValues[0].length/2] == noDataValue) {
            return noDataValue;
        }

        // y is the pixel amplitude or intensity and x is the pixel reflectance before degradation
        final double meanY = getLocalMeanValue(neighborPixelValues, noDataValue);
        if (meanY == noDataValue) {
            return noDataValue;
        }

        final double varY = getLocalVarianceValue(meanY, neighborPixelValues, noDataValue);
        if (varY == 0.0) {
            return meanY;
        }

        if (varY == noDataValue) {
            return noDataValue;
        }

        final double sigmaV = getLocalNoiseVarianceValue(neighborPixelValues, noDataValue);
        double varX = (varY - meanY * meanY * sigmaV) / (1 + sigmaV);
        if (varX < 0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b * (neighborPixelValues[3][3] - meanY);
    }

    /**
     * Compute filtered pixel value using refined Lee filter.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The filtered pixel value.
     */
    private static double computePixelValueUsingEdgeDetection(
            final double[][] neighborPixelValues, final double noDataValue,
            final double[][] subAreaMeans, final double[] gradients) {

        int direction = 0;
        double maxGradient = -Double.MAX_VALUE;
        for (int i = 0; i < gradients.length; i++) {
            if (maxGradient < gradients[i]) {
                maxGradient = gradients[i];
                direction = i;
            }
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

        final double meanY = getMeanValue(pixels, pixels.length, noDataValue);
        final double varY = getVarianceValue(pixels, pixels.length, meanY, noDataValue);
        if (varY == 0.0) {
            return 0.0;
        }
        final double sigmaV = getLocalNoiseVarianceValue(neighborPixelValues, noDataValue);
        double varX = (varY - meanY * meanY * sigmaV) / (1 + sigmaV);
        if (varX < 0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b * (neighborPixelValues[3][3] - meanY);
    }

    /**
     * Compute local mean for pixels in the neighborhood.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The local mean.
     */
    private static double getLocalMeanValue(final double[][] neighborPixelValues, final double noDataValue) {

        int k = 0;
        double mean = 0;
        for (int j = 0; j < neighborPixelValues.length; ++j) {
            for (int i = 0; i < neighborPixelValues[0].length; ++i) {
                if (neighborPixelValues[j][i] != noDataValue) {
                    mean += neighborPixelValues[j][i];
                    k++;
                }
            }
        }

        if (k > 0) {
            return mean / k;
        }

        return noDataValue;
    }

    /**
     * Compute local variance for pixels in the neighborhood.
     *
     * @param mean                The mean value for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The local variance.
     */
    private static double getLocalVarianceValue(
            final double mean, final double[][] neighborPixelValues, final double noDataValue) {

        int k = 0;
        double var = 0.0;
        for (int j = 0; j < neighborPixelValues.length; ++j) {
            for (int i = 0; i < neighborPixelValues[0].length; ++i) {
                if (neighborPixelValues[j][i] != noDataValue) {
                    final double diff = neighborPixelValues[j][i] - mean;
                    var += diff * diff;
                    k++;
                }
            }
        }

        if (k > 1) {
            return var / (k - 1);
        }

        return noDataValue;
    }

    /**
     * Compute local noise variance for pixels in the neighborhood.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The local noise variance.
     */
    private static double getLocalNoiseVarianceValue(final double[][] neighborPixelValues, final double noDataValue) {

        final double[] subAreaVariances = new double[9];
        final double[] subArea = new double[9];
        int numSubArea = 0;
        for (int j = 0; j < 3; j++) {
            final int y0 = 2 * j;
            for (int i = 0; i < 3; i++) {
                final int x0 = 2 * i;

                int k = 0;
                for (int y = y0; y < y0 + 3; y++) {
                    final int yy = (y - y0) * 3;
                    for (int x = x0; x < x0 + 3; x++) {
                        if (neighborPixelValues[y][x] != noDataValue) {
                            subArea[yy + (x - x0)] = neighborPixelValues[y][x];
                            k++;
                        }
                    }
                }

                if (k == 9) {
                    final double subAreaMean = getMeanValue(subArea, k, noDataValue);
                    if (subAreaMean > 0) {
                        subAreaVariances[numSubArea] =
                                getVarianceValue(subArea, k, subAreaMean, noDataValue) / (subAreaMean * subAreaMean);
                    } else {
                        subAreaVariances[numSubArea] = 0.0;
                    }
                    numSubArea++;
                }
            }
        }

        if (numSubArea < 1) {
            return 0.0;
        }
        Arrays.sort(subAreaVariances, 0, numSubArea - 1);
        final int numSubAreaForAvg = Math.min(5, numSubArea);
        double avg = 0.0;
        for (int n = 0; n < numSubAreaForAvg; n++) {
            avg += subAreaVariances[n];
        }
        return avg / numSubAreaForAvg;
    }

    /**
     * Compute mean values for the 9 3x3 sub-areas in the 7x7 neighborhood.
     *
     * @param neighborPixelValues The pixel values in the 7x7 neighborhood.
     * @param subAreaMeans        The 9 mean values.
     */
    private static void computeSubAreaMeans(
            final double[][] neighborPixelValues, double[][] subAreaMeans) {

        for (int j = 0; j < 3; j++) {
            final int y0 = 2 * j;
            for (int i = 0; i < 3; i++) {
                final int x0 = 2 * i;

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

    private static void computeGradients(final double[][] subAreaMeans, final double[] gradients) {

        gradients[0] = Math.abs(subAreaMeans[1][0] - subAreaMeans[1][2]);
        gradients[1] = Math.abs(subAreaMeans[0][2] - subAreaMeans[2][0]);
        gradients[2] = Math.abs(subAreaMeans[0][1] - subAreaMeans[2][1]);
        gradients[3] = Math.abs(subAreaMeans[0][0] - subAreaMeans[2][2]);
    }

    /**
     * Get pixel values from the non-edge area indicated by the given direction.
     *
     * @param neighborPixelValues The pixel values in the 7x7 neighborhood.
     * @param d                   The direction index.
     * @param pixels              The array of pixels.
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
            }
            case 1: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = y; x < 7; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 2: {

                int k = 0;
                for (int y = 0; y < 4; y++) {
                    for (int x = 0; x < 7; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 3: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = 0; x < 7 - y; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 4: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = 0; x < 4; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 5: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = 0; x < y + 1; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 6: {

                int k = 0;
                for (int y = 3; y < 7; y++) {
                    for (int x = 0; x < 7; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 7: {

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

    private void setLeeSigmaParameters() {

        numLooks = Integer.parseInt(numLooksStr);

        switch (windowSize) {
            case WINDOW_SIZE_5x5:
                filterSize = 5;
                break;
            case WINDOW_SIZE_7x7:
                filterSize = 7;
                break;
            case WINDOW_SIZE_9x9:
                filterSize = 9;
                break;
            case WINDOW_SIZE_11x11:
                filterSize = 11;
                break;
            default:
                throw new OperatorException("Unknown window size: " + windowSize);
        }

        halfSizeX = filterSize/2;
        halfSizeY = halfSizeX;

        switch (targetWindowSizeStr) {
            case WINDOW_SIZE_3x3:
                targetWindowSize = 3;
                break;
            case WINDOW_SIZE_5x5:
                targetWindowSize = 5;
                break;
            default:
                throw new OperatorException("Unknown target window size: " + targetWindowSizeStr);
        }

        halfTargetWindowSize = targetWindowSize / 2;

        ISigmaV = 1.0 / Math.sqrt(numLooks);
        ISigmaVSqr = ISigmaV * ISigmaV;

        ASigmaV = 0.5227 / Math.sqrt(numLooks);
        ASigmaVSqr = ASigmaV * ASigmaV;

        setSigmaRange(sigmaStr);
    }

    private void setSigmaRange(final String sigmaStr) throws OperatorException {

        switch (sigmaStr) {
            case SIGMA_50_PERCENT:
                sigma = 5;
                break;
            case SIGMA_60_PERCENT:
                sigma = 6;
                break;
            case SIGMA_70_PERCENT:
                sigma = 7;
                break;
            case SIGMA_80_PERCENT:
                sigma = 8;
                break;
            case SIGMA_90_PERCENT:
                sigma = 9;
                break;
            default:
                throw new OperatorException("Unknown sigma: " + sigmaStr);
        }

        if (numLooks == 1) {

            if (sigma == 5) {
                I1 = 0.436;
                I2 = 1.920;
                ISigmaVP = 0.4057;
            } else if (sigma == 6) {
                I1 = 0.343;
                I2 = 2.210;
                ISigmaVP = 0.4954;
            } else if (sigma == 7) {
                I1 = 0.254;
                I2 = 2.582;
                ISigmaVP = 0.5911;
            } else if (sigma == 8) {
                I1 = 0.168;
                I2 = 3.094;
                ISigmaVP = 0.6966;
            } else if (sigma == 9) {
                I1 = 0.084;
                I2 = 3.941;
                ISigmaVP = 0.8191;
            }

        } else if (numLooks == 2) {

            if (sigma == 5) {
                I1 = 0.582;
                I2 = 1.584;
                ISigmaVP = 0.2763;
            } else if (sigma == 6) {
                I1 = 0.501;
                I2 = 1.755;
                ISigmaVP = 0.3388;
            } else if (sigma == 7) {
                I1 = 0.418;
                I2 = 1.972;
                ISigmaVP = 0.4062;
            } else if (sigma == 8) {
                I1 = 0.327;
                I2 = 2.260;
                ISigmaVP = 0.4810;
            } else if (sigma == 9) {
                I1 = 0.221;
                I2 = 2.744;
                ISigmaVP = 0.5699;
            }

        } else if (numLooks == 3) {

            if (sigma == 5) {
                I1 = 0.652;
                I2 = 1.458;
                ISigmaVP = 0.2222;
            } else if (sigma == 6) {
                I1 = 0.580;
                I2 = 1.586;
                ISigmaVP = 0.2736;
            } else if (sigma == 7) {
                I1 = 0.505;
                I2 = 1.751;
                ISigmaVP = 0.3280;
            } else if (sigma == 8) {
                I1 = 0.419;
                I2 = 1.965;
                ISigmaVP = 0.3892;
            } else if (sigma == 9) {
                I1 = 0.313;
                I2 = 2.320;
                ISigmaVP = 0.4624;
            }

        } else if (numLooks == 4) {

            if (sigma == 5) {
                I1 = 0.694;
                I2 = 1.385;
                ISigmaVP = 0.1921;
            } else if (sigma == 6) {
                I1 = 0.630;
                I2 = 1.495;
                ISigmaVP = 0.2348;
            } else if (sigma == 7) {
                I1 = 0.560;
                I2 = 1.627;
                ISigmaVP = 0.2825;
            } else if (sigma == 8) {
                I1 = 0.480;
                I2 = 1.804;
                ISigmaVP = 0.3354;
            } else if (sigma == 9) {
                I1 = 0.378;
                I2 = 2.094;
                ISigmaVP = 0.3991;
            }
        }

        ISigmaVPSqr = ISigmaVP * ISigmaVP;

        if (numLooks == 1) {

            if (sigma == 5) {
                A1 = 0.653997;
                A2 = 1.40002;
                ASigmaVP = 0.208349;
            } else if (sigma == 6) {
                A1 = 0.578998;
                A2 = 1.50601;
                ASigmaVP = 0.255358;
            } else if (sigma == 7) {
                A1 = 0.496999;
                A2 = 1.63201;
                ASigmaVP = 0.305303;
            } else if (sigma == 8) {
                A1 = 0.403999;
                A2 = 1.79501;
                ASigmaVP = 0.361078;
            } else if (sigma == 9) {
                A1 = 0.286;
                A2 = 2.04301;
                ASigmaVP = 0.426375;
            }

        } else if (numLooks == 2) {

            if (sigma == 5) {
                A1 = 0.76;
                A2 = 1.263;
                ASigmaVP = 0.139021;
            } else if (sigma == 6) {
                A1 = 0.705;
                A2 = 1.332;
                ASigmaVP = 0.169777;
            } else if (sigma == 7) {
                A1 = 0.643;
                A2 = 1.412;
                ASigmaVP = 0.206675;
            } else if (sigma == 8) {
                A1 = 0.568;
                A2 = 1.515;
                ASigmaVP = 0.244576;
            } else if (sigma == 9) {
                A1 = 0.467;
                A2 = 1.673;
                ASigmaVP = 0.29107;
            }

        } else if (numLooks == 3) {

            if (sigma == 5) {
                A1 = 0.806;
                A2 = 1.21;
                ASigmaVP = 0.109732;
            } else if (sigma == 6) {
                A1 = 0.76;
                A2 = 1.263;
                ASigmaVP = 0.138001;
            } else if (sigma == 7) {
                A1 = 0.708;
                A2 = 1.327;
                ASigmaVP = 0.163686;
            } else if (sigma == 8) {
                A1 = 0.645;
                A2 = 1.408;
                ASigmaVP = 0.19597;
            } else if (sigma == 9) {
                A1 = 0.557;
                A2 = 1.531;
                ASigmaVP = 0.234219;
            }

        } else if (numLooks == 4) {

            if (sigma == 5) {
                A1 = 0.832;
                A2 = 1.179;
                ASigmaVP = 0.0894192;
            } else if (sigma == 6) {
                A1 = 0.793;
                A2 = 1.226;
                ASigmaVP = 0.112018;
            } else if (sigma == 7) {
                A1 = 0.747;
                A2 = 1.279;
                ASigmaVP = 0.139243;
            } else if (sigma == 8) {
                A1 = 0.691;
                A2 = 1.347;
                ASigmaVP = 0.167771;
            } else if (sigma == 9) {
                A1 = 0.613;
                A2 = 1.452;
                ASigmaVP = 0.839;
            }
        }
        ASigmaVPSqr = ASigmaVP * ASigmaVP;
    }

    /**
     * Filter the given tile of image with refined Lee filter.
     *
     * @param srcData1    The source ProductData for the 1st band.
     * @param srcData2    The source ProductData for the 2nd band.
     * @param tgtData     target ProductData
     * @param noDataValue The place holder for no data.
     * @param unit        Unit for the 1st band.
     * @param srcIndex    The source tile index.
     * @param tgtIndex    The target tile index.
     * @param x0          X coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param y0          Y coordinate for the upper-left point of the target_Tile_Rectangle.
     * @param w           Width for the target_Tile_Rectangle.
     * @param h           Height for the target_Tile_Rectangle.
     */
    private void computeLeeSigma(final ProductData srcData1, final ProductData srcData2, final ProductData tgtData,
                                 final double noDataValue, final Unit.UnitType unit,
                                 final TileIndex srcIndex, final TileIndex tgtIndex,
                                 final int x0, final int y0, final int w, final int h,
                                 final Rectangle sourceRectangle, final Tile sourceTile) {

        final int maxY = y0 + h;
        final int maxX = x0 + w;

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
		
		double sigmaVSqr, sigmaVPSqr, sigmaRangeLow, sigmaRangeHigh;
		if (unit == Unit.UnitType.AMPLITUDE) {
			sigmaVSqr = ASigmaVSqr;
			sigmaVPSqr = ASigmaVPSqr;
			sigmaRangeLow = A1;
			sigmaRangeHigh = A2;
		} else {
			sigmaVSqr = ISigmaVSqr;
			sigmaVPSqr = ISigmaVPSqr;
			sigmaRangeLow = I1;
			sigmaRangeHigh = I2;
		}

        final double z98 = computeZ98Values(srcIndex, sourceRectangle, noDataValue, unit, srcData1, srcData2);

        final boolean[][] isPointTarget = new boolean[h][w];
        final double[][] targetWindow = new double[targetWindowSize][targetWindowSize];
        final double[][] filterWindow = new double[filterSize][filterSize];
        double[] pixelsSelected = null;

        int xx, yy;
        for (int y = y0; y < maxY; ++y) {
            yy = y - y0;
            tgtIndex.calculateStride(y);
            srcIndex.calculateStride(y);

            for (int x = x0; x < maxX; ++x) {
                xx = x - x0;
                final int tgtIdx = tgtIndex.getIndex(x);
                final int srcIdx = srcIndex.getIndex(x);

                final double v = getPixelValue(srcIdx, noDataValue, unit, srcData1, srcData2);

                if (isPointTarget[yy][xx]) {
                    tgtData.setElemDoubleAt(tgtIdx, v);
                    continue;
                }

                if (y - halfSizeY < sy0 || y + halfSizeY > sy0 + sh - 1 ||
                        x - halfSizeX < sx0 || x + halfSizeX > sx0 + sw - 1) {

                    getWindowPixels(
                            x, y, sx0, sy0, sw, sh, sourceTile, noDataValue, unit, srcData1, srcData2, filterWindow);

                    pixelsSelected = getValidPixels(filterWindow, noDataValue);

                    final double vEst = computeMMSEEstimate(v, pixelsSelected, sigmaVSqr, noDataValue);

                    tgtData.setElemDoubleAt(tgtIdx, vEst);
                    continue;
                }

                getWindowPixels(x, y, sx0, sy0, sw, sh, sourceTile, noDataValue, unit, srcData1, srcData2, targetWindow);

                if (checkPointTarget(x, y, z98, targetWindow, isPointTarget, x0, y0, w, h, noDataValue)) {
                    tgtData.setElemDoubleAt(tgtIdx, v);
                    continue;
                }

                pixelsSelected = getValidPixels(targetWindow, noDataValue);
                final double meanEst = computeMMSEEstimate(v, pixelsSelected, sigmaVSqr, noDataValue);
                double[] sigmaRange = {meanEst * sigmaRangeLow, meanEst * sigmaRangeHigh};

                getWindowPixels(x, y, sx0, sy0, sw, sh, sourceTile, noDataValue, unit, srcData1, srcData2, filterWindow);

                pixelsSelected = selectPixelsInSigmaRange(sigmaRange, filterWindow, noDataValue);
                if (pixelsSelected.length == 0) {
                    tgtData.setElemDoubleAt(tgtIdx, v);
                    continue;
                }

                final double vEst = computeMMSEEstimate(v, pixelsSelected, sigmaVPSqr, noDataValue);
                tgtData.setElemDoubleAt(tgtIdx, vEst);
            }
        }
    }

    private double computeZ98Values(final TileIndex srcIndex, final Rectangle sourceRectangle,
                                    final double noDataValue, final Unit.UnitType unit,
                                    final ProductData srcData1, final ProductData srcData2) {

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;
        final int z98Index = (int) (sw * sh * 0.98) - 1;

        double[] pixelValues = new double[sw * sh];
        int k = 0;
        for (int y = sy0; y < maxY; y++) {
            srcIndex.calculateStride(y);
            for (int x = sx0; x < maxX; x++) {
                pixelValues[k++] = getPixelValue(srcIndex.getIndex(x), noDataValue, unit, srcData1, srcData2);
            }
        }

        Arrays.sort(pixelValues);
        return pixelValues[z98Index];
    }

    private double getPixelValue(final int index, final double noDataValue, final Unit.UnitType unit,
                                 final ProductData srcData1, final ProductData srcData2) {

        if (unit == Unit.UnitType.REAL || unit == Unit.UnitType.IMAGINARY) {
            final double I = srcData1.getElemDoubleAt(index);
            final double Q = srcData2.getElemDoubleAt(index);
            if (I != noDataValue && Q != noDataValue) {
                return I * I + Q * Q;
            } else {
                return noDataValue;
            }
        } else {
            return srcData1.getElemDoubleAt(index);
        }
    }

    private void getWindowPixels(final int x, final int y, final int sx0, final int sy0, final int sw, final int sh,
                                 final Tile sourceTile, final double noDataValue, final Unit.UnitType unit,
                                 final ProductData srcData1, final ProductData srcData2, final double[][] windowPixel) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        final int windowSize = windowPixel.length;
        final int halfWindowSize = windowSize / 2;

        int yy, xx;
        for (int j = 0; j < windowSize; j++) {
            yy = y - halfWindowSize + j;
            srcIndex.calculateStride(yy);
            for (int i = 0; i < windowSize; i++) {
                xx = x - halfWindowSize + i;
                if (yy >= sy0 && yy <= sy0 + sh - 1 && xx >= sx0 && xx <= sx0 + sw - 1) {
                    windowPixel[j][i] = getPixelValue(srcIndex.getIndex(xx), noDataValue, unit, srcData1, srcData2);
                } else {
                    windowPixel[j][i] = noDataValue;
                }
            }
        }
    }

    private double[] getValidPixels(final double[][] filterWindow, final double noDataValue) {

        final List<Double> pixelsSelected = new ArrayList<>();
        final int nRows = filterWindow.length;
        final int nCols = filterWindow[0].length;
        for (int j = 0; j < nRows; j++) {
            for (int i = 0; i < nCols; i++) {
                if (filterWindow[j][i] != noDataValue) {
                    pixelsSelected.add(filterWindow[j][i]);
                }
            }
        }
        return Doubles.toArray(pixelsSelected);
    }

    private boolean checkPointTarget(final int x, final int y, final double z98, final double[][] targetWindow,
                                     final boolean[][] isPointTarget, final int x0, final int y0, final int w,
                                     final int h, final double noDataValue) {

        if (targetWindow[halfTargetWindowSize][halfTargetWindowSize] > z98) {
            if (getClusterSize(z98, targetWindow, noDataValue) > targetSize) {
                markClusterPixels(x, y, isPointTarget, z98, targetWindow, x0, y0, w, h, noDataValue);
                return true;
            }
        }

        return false;
    }

    private int getClusterSize(final double threshold, final double[][] targetWindow, final double noDataValue) {

        int clusterSize = 0;
        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                if (targetWindow[j][i] != noDataValue && targetWindow[j][i] > threshold) {
                    clusterSize++;
                }
            }
        }
        return clusterSize;
    }

    private void markClusterPixels( final int x, final int y,
            boolean[][] isPointTarget, final double threshold, final double[][] targetWindow,
            final int x0, final int y0, final int w, final int h, final double noDataValue) {

        final int windowSize = targetWindow.length;
        final int halfWindowSize = windowSize / 2;

        int yy, xx;
        for (int j = 0; j < targetWindowSize; j++) {
            yy = y - halfWindowSize + j;
            for (int i = 0; i < targetWindowSize; i++) {
                xx = x - halfWindowSize + i;
                if (targetWindow[j][i] != noDataValue && targetWindow[j][i] > threshold &&
                        yy >= y0 && yy < y0 + h && xx >= x0 && xx < x0 + w) {
                    isPointTarget[yy - y0][xx - x0] = true;
                }
            }
        }
    }

    private static double computeMMSEWeight(
	final double[] dataArray, final double meanY, final double sigmaVSqr, final double noDataValue) {

        final double varY = getVarianceValue(dataArray, dataArray.length, meanY, noDataValue);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX  = (varY - meanY * meanY * sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }
		
        return varX / varY;
    }

    private double[] selectPixelsInSigmaRange(
            final double[] sigmaRange, final double[][] filterWindow, final double noDataValue) {

        final List<Double> pixelsSelected = new ArrayList<>();
        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindow[j][i] != noDataValue && filterWindow[j][i] >= sigmaRange[0] &&
                        filterWindow[j][i] <= sigmaRange[1]) {
                    pixelsSelected.add(filterWindow[j][i]);
                }
            }
        }

        return Doubles.toArray(pixelsSelected);
    }

    private double computeMMSEEstimate(final double centerPixelValue, final double[] dataArray,
                                       final double sigmaVSqr, final double noDataValue) {

        final double mean = getMeanValue(dataArray, dataArray.length, noDataValue);

        final double b = computeMMSEWeight(dataArray, mean, sigmaVSqr, noDataValue);

        return (1 - b) * mean + b * centerPixelValue;
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SpeckleFilterOp.class);
        }
    }
}

