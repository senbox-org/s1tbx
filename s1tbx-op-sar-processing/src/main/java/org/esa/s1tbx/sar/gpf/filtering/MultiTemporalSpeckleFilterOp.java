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
import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilters.*;
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
import org.esa.snap.engine_utilities.gpf.FilterWindow;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies Multitemporal Speckle Filtering to multitemporal images.
 * <p/>
 * For a sequence of n registered multitemporal PRI images, with intensity at position (x, y) in image k
 * denoted by Ik(x, y), the goal of temporal filtering is to combine them linearly such that the n output
 * images Jk(x, y) meeting the following two conditions:
 * <p/>
 * 1. Jk is unbiased (i.e. E[Jk] = E[Ik], where E[] denotes expected value, so that the filtering does not
 * distort the sigma0 values).
 * <p/>
 * 2. Jk has minimum variance, so that speckle is minimized.
 * <p/>
 * The following equation has been implemented:
 * <p/>
 * Jk(x, y) = E[Ik]*(I1(x, y)/E[I1] + ... + In(x, y)/E[In])/n
 * <p/>
 * where E[I] is the local mean value of pixels in a user selected window centered at (x, y) in image I.
 * The window size can be 3x3, 5x5, 7x7, 9x9 or 11x11.
 * <p/>
 * The operator has the following two preprocessing steps:
 * <p/>
 * 1. The first step is calibration in which ?0 is derived from the digital number at each pixel. This
 * ensures that values of from different times and in different parts of the image are comparable.
 * <p/>
 * 2. The second is registration of the images in the multitemporal sequence.
 * <p/>
 * Here it is assumed that preprocessing has been performed before applying this operator. The input to
 * the operator is assumed to be a product with multiple calibrated and co-registrated bands.
 * <p/>
 * Reference:
 * [1] S. Quegan, T. L. Toan, J. J. Yu, F. Ribbes and N. Floury, "Multitemporal ERS SAR Analysis Applied to
 * Forest Mapping", IEEE Transactions on Geoscience and Remote Sensing, vol. 38, no. 2, March 2000.
 */

@OperatorMetadata(alias = "Multi-Temporal-Speckle-Filter",
        category = "Radar/Speckle Filtering",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Speckle Reduction using Multitemporal Filtering")
public class MultiTemporalSpeckleFilterOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {SpeckleFilterOp.NONE, SpeckleFilterOp.BOXCAR_SPECKLE_FILTER,
            SpeckleFilterOp.MEDIAN_SPECKLE_FILTER, SpeckleFilterOp.FROST_SPECKLE_FILTER,
            SpeckleFilterOp.GAMMA_MAP_SPECKLE_FILTER, SpeckleFilterOp.LEE_SPECKLE_FILTER,
            SpeckleFilterOp.LEE_REFINED_FILTER, SpeckleFilterOp.LEE_SIGMA_FILTER, SpeckleFilterOp.IDAN_FILTER},
            defaultValue = SpeckleFilterOp.LEE_SIGMA_FILTER, label = "Filter")
    private String filter;

    @Parameter(description = "The kernel x dimension", interval = "(1, 100]", defaultValue = "3", label = "Size X")
    private int filterSizeX = 3;

    @Parameter(description = "The kernel y dimension", interval = "(1, 100]", defaultValue = "3", label = "Size Y")
    private int filterSizeY = 3;

    @Parameter(description = "The damping factor (Frost filter only)", interval = "(0, 100]", defaultValue = "2",
            label = "Frost Damping Factor")
    private int dampingFactor = 2;

//    @Parameter(description = "The edge threshold (Refined Lee filter only)", interval = "(0, *)",
//            defaultValue = "5000", label = "Edge detection threshold")
//    private double edgeThreshold = 5000.0;

    @Parameter(defaultValue = "false", label = "Estimate Eqivalent Number of Looks")
    private boolean estimateENL = true;

    @Parameter(description = "The number of looks", interval = "(0, *)", defaultValue = "1.0",
            label = "Number of looks")
    private double enl = 1.0;

    @Parameter(valueSet = {SpeckleFilterOp.NUM_LOOKS_1, SpeckleFilterOp.NUM_LOOKS_2, SpeckleFilterOp.NUM_LOOKS_3,
            SpeckleFilterOp.NUM_LOOKS_4}, defaultValue = SpeckleFilterOp.NUM_LOOKS_1, label = "Number of Looks")
    private String numLooksStr = SpeckleFilterOp.NUM_LOOKS_1;

    @Parameter(valueSet = {FilterWindow.SIZE_5x5, FilterWindow.SIZE_7x7, FilterWindow.SIZE_9x9, FilterWindow.SIZE_11x11,
            FilterWindow.SIZE_13x13, FilterWindow.SIZE_15x15, FilterWindow.SIZE_17x17},
            defaultValue = FilterWindow.SIZE_7x7, label = "Window Size")
    private String windowSize = FilterWindow.SIZE_7x7; // window size for all filters

    @Parameter(valueSet = {FilterWindow.SIZE_3x3, FilterWindow.SIZE_5x5},
            defaultValue = FilterWindow.SIZE_3x3, label = "Point target window Size")
    private String targetWindowSizeStr = FilterWindow.SIZE_3x3; // window size for point target determination in Lee sigma

    @Parameter(valueSet = {SpeckleFilterOp.SIGMA_50_PERCENT, SpeckleFilterOp.SIGMA_60_PERCENT,
            SpeckleFilterOp.SIGMA_70_PERCENT, SpeckleFilterOp.SIGMA_80_PERCENT, SpeckleFilterOp.SIGMA_90_PERCENT},
            defaultValue = SpeckleFilterOp.SIGMA_90_PERCENT, label = "Point target window Size")
    private String sigmaStr = SpeckleFilterOp.SIGMA_90_PERCENT; // sigma value in Lee sigma

    @Parameter(description = "The Adaptive Neighbourhood size", interval = "(1, 200]", defaultValue = "50",
            label = "Adaptive Neighbourhood Size")
    private int anSize = 50;

    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<>();

    private SpeckleFilter speckleFilter;
    private static final String PRODUCT_SUFFIX = "_Spk";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public MultiTemporalSpeckleFilterOp() {
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

            createTargetProduct();

            speckleFilter = createFilter();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
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
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        if(filter.equals(SpeckleFilterOp.NONE)) {
            final Band[] selectedBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
            for (Band srcBand : selectedBands) {
                if (srcBand instanceof VirtualBand) {
                    ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBand.getName());
                } else {
                    ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                }
            }
        } else {
            addSelectedBands();
        }

        updateTargetProductMetadata();
    }

    /**
     * Add user selected bands to target product.
     */
    private void addSelectedBands() {

        final InputProductValidator validator = new InputProductValidator(sourceProduct);
        if (sourceBandNames == null || sourceBandNames.length == 0 && validator.isComplex()) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (band.getUnit().contains("intensity"))
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }
        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);

        if (sourceBands.length <= 1) {
            throw new OperatorException("Multitemporal filtering cannot be applied with one source band. Select more bands.");
        }

        for (Band srcBand : sourceBands) {
            final String unit = srcBand.getUnit();
            if (unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            if (unit.contains(Unit.PHASE) || unit.contains(Unit.IMAGINARY) || unit.contains(Unit.REAL)) {
                throw new OperatorException("Please select amplitude or intensity bands.");
            } else {
                final Band targetBand = new Band(srcBand.getName(),
                        ProductData.TYPE_FLOAT32,
                        sourceProduct.getSceneRasterWidth(),
                        sourceProduct.getSceneRasterHeight());

                targetBand.setUnit(unit);
                targetProduct.addBand(targetBand);
                final String[] srcBandNames = {srcBand.getName()};
                targetBandNameToSourceBandName.put(targetBand.getName(), srcBandNames);
            }
        }
    }

    private SpeckleFilter createFilter() {

        switch (filter) {
            case SpeckleFilterOp.BOXCAR_SPECKLE_FILTER:
                return new Boxcar(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName);

            case SpeckleFilterOp.MEDIAN_SPECKLE_FILTER:
                return new Median(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName);

            case SpeckleFilterOp.FROST_SPECKLE_FILTER:
                return new Frost(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName, dampingFactor);

            case SpeckleFilterOp.GAMMA_MAP_SPECKLE_FILTER:
                return new GammaMap(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName, estimateENL, enl);

            case SpeckleFilterOp.LEE_SPECKLE_FILTER:
                return new Lee(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName, estimateENL, enl);

            case SpeckleFilterOp.LEE_REFINED_FILTER:
                return new RefinedLee(this, sourceProduct, targetProduct, targetBandNameToSourceBandName);

            case SpeckleFilterOp.LEE_SIGMA_FILTER:
                return new LeeSigma(this, sourceProduct, targetProduct, targetBandNameToSourceBandName,
                        numLooksStr, windowSize, targetWindowSizeStr, sigmaStr);

            case SpeckleFilterOp.IDAN_FILTER:
                return new IDAN(this, sourceProduct, targetProduct, targetBandNameToSourceBandName,
                        numLooksStr, anSize);

            default:
                return null;
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int yMax = y0 + h;
        final int xMax = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            final Band[] targetBands = targetProduct.getBands();
            final int numBands = targetBands.length;

            final List<double[][]> filteredTileList = new ArrayList<>();;
            double[][] sum = new double[h][w];
            int[][] count = new int[h][w];
            for (Band tgtBand : targetBands) {
                final Band srcBand = sourceProduct.getBand(tgtBand.getName());
                final Tile srcTile = getSourceTile(srcBand, targetRectangle);
                final ProductData srcData = srcTile.getDataBuffer();
                final double bandNoDataValues = srcBand.getNoDataValue();
                final String[] srcBandNames = {srcBand.getName()};

                final double[][] filteredTile = speckleFilter.performFiltering(x0, y0, w, h, srcBandNames);

                filteredTileList.add(filteredTile);

                for (int y = y0; y < yMax; ++y) {
                    final int yy = y - y0;
                    for (int x = x0; x < xMax; ++x) {
                        final int xx = x - x0;
                        if (filteredTile[yy][xx] != 0.0) {
                            final int sourceIndex = srcTile.getDataBufferIndex(x, y);
                            final double srcDataValue = srcData.getElemDoubleAt(sourceIndex);
                            if (srcDataValue != bandNoDataValues) {
                                sum[yy][xx] += srcDataValue / filteredTile[yy][xx];
                                count[yy][xx]++;
                            }
                        }
                    }
                }
            }

            for (int yy = 0; yy < h; ++yy) {
                for (int xx = 0; xx < w; ++xx) {
                    if (count[yy][xx] > 0) {
                        sum[yy][xx] /= count[yy][xx];
                    }
                }
            }

            for (int i = 0; i < numBands; i++) {
                Tile targetTile = targetTiles.get(targetBands[i]);
                final ProductData targetData = targetTile.getDataBuffer();
                final double[][] filteredTile = filteredTileList.get(i);
                for (int y = y0; y < yMax; y++) {
                    final int yy = y - y0;
                    for (int x = x0; x < xMax; x++) {
                        final int xx = x - x0;
                        final int targetIndex = targetTile.getDataBufferIndex(x, y);
                        targetData.setElemDoubleAt(targetIndex, filteredTile[yy][xx] * sum[yy][xx]);
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
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
            super(MultiTemporalSpeckleFilterOp.class);
        }
    }
}

